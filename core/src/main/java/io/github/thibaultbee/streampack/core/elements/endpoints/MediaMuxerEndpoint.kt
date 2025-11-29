/*
 * Copyright (C) 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException

/**
 * An [IEndpointInternal] implementation of the [MediaMuxer].
 */
class MediaMuxerEndpoint(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) : IEndpointInternal {
    private var mediaMuxer: MediaMuxer? = null
    private val mutex = Mutex()

    private var containerType: MediaContainerType? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    private var state = State.IDLE

    /**
     * Map streamId to MediaMuxer trackId
     */
    private val streamIdToTrackId = mutableMapOf<Int, Int>()
    private var numOfStreams = 0

    override val info: IEndpoint.IEndpointInfo
        get() = containerType?.let { getInfo(it) }
            ?: throw IllegalStateException("Endpoint is not opened")

    override fun getInfo(type: MediaDescriptor.Type) = Companion.getInfo(type)

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    override val throwableFlow: StateFlow<Throwable?> = MutableStateFlow(null).asStateFlow()

    override suspend fun open(descriptor: MediaDescriptor) = mutex.withLock {
        when (state) {
            State.PENDING_START, State.STARTED -> {
                throw IllegalStateException("Can't open while streaming")
            }

            State.PENDING_RELEASE, State.RELEASED -> {
                throw IllegalStateException("Muxer is released")
            }

            State.PENDING_STOP, State.STOPPED, State.ERROR -> {
                throw IllegalStateException("Muxer is stopping or stopped. Close it and open a new one.")
            }

            State.IDLE, State.CONFIGURED -> {
                // Ok
            }
        }
        if (isOpenFlow.value) {
            Logger.w(TAG, "MediaMuxerEndpoint is already opened")
            return
        }

        require((descriptor.type.sinkType == MediaSinkType.FILE) || (descriptor.type.sinkType == MediaSinkType.CONTENT)) { "MediaDescriptor must have a path" }
        val containerType = descriptor.type.containerType
        require(
            (containerType == MediaContainerType.MP4) || (containerType == MediaContainerType.THREEGP) || (containerType == MediaContainerType.WEBM) || (containerType == MediaContainerType.OGG)
        ) {
            "Unsupported container type: $containerType"
        }
        this.containerType = containerType

        try {
            when (descriptor.type.sinkType) {
                MediaSinkType.FILE -> {
                    val path = descriptor.uri.path
                        ?: throw IllegalStateException("Could not get path from uri: ${descriptor.uri}")
                    mediaMuxer = MediaMuxer(path, containerType.outputFormat)
                }

                MediaSinkType.CONTENT -> {
                    fileDescriptor = context.contentResolver.openFileDescriptor(
                        descriptor.uri, "w"
                    )?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mediaMuxer = MediaMuxer(
                                this.fileDescriptor, containerType.outputFormat
                            )
                        } else {
                            throw IllegalStateException("Using content sink for API < 26 is not supported. Use file sink instead.")
                        }
                    }
                        ?: throw IllegalStateException("Could not open file descriptor for uri: ${descriptor.uri}")
                }

                else -> throw InvalidParameterException("Unsupported sink type: ${descriptor.type.sinkType}")
            }
        } catch (t: Throwable) {
            this.containerType = null
            mediaMuxer?.release()
            mediaMuxer = null
            fileDescriptor?.close()
            fileDescriptor = null
            throw t
        }

        _isOpenFlow.emit(true)
    }

    override suspend fun write(
        closeableFrame: FrameWithCloseable, streamPid: Int
    ) = withContext(ioDispatcher) {
        val frame = closeableFrame.frame
        mutex.withLock {
            try {
                if (state != State.STARTED && state != State.PENDING_START) {
                    Logger.w(TAG, "Trying to write while not started. Current state: $state")
                    return@withContext
                }

                val mediaMuxer = requireNotNull(mediaMuxer) { "MediaMuxer is not initialized" }

                if ((state == State.PENDING_START) && (streamIdToTrackId.size < numOfStreams)) {
                    addTrack(mediaMuxer, streamPid, frame.format)
                    if (streamIdToTrackId.size == numOfStreams) {
                        mediaMuxer.start()
                        setState(State.STARTED)
                    }
                }

                if (state == State.STARTED) {
                    val trackId = streamIdToTrackId[streamPid]
                        ?: throw IllegalStateException("Could not find trackId for streamPid $streamPid: ${frame.format}")
                    val info = BufferInfo().apply {
                        set(
                            0,
                            frame.rawBuffer.remaining(),
                            frame.ptsInUs,
                            if (frame.isKeyFrame) BUFFER_FLAG_KEY_FRAME else 0
                        )
                    }
                    try {
                        mediaMuxer.writeSampleData(trackId, frame.rawBuffer, info)
                    } catch (e: IllegalStateException) {
                        Logger.w(TAG, "MediaMuxer is in an illegal state. ${e.message}")
                    }
                }
            } catch (t: Throwable) {
                Logger.e(TAG, "Error while writing frame: ${t.message}")
                throw t
            } finally {
                closeableFrame.close()
            }
        }
    }

    override suspend fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        require(streamConfigs.isNotEmpty()) { "At least one stream config must be provided" }
        return mutex.withLock {
            require(state != State.RELEASED) { "Muxer is released" }
            try {
                requireNotNull(mediaMuxer) { "MediaMuxer is not initialized" }
                /**
                 * We can't addTrack here because we don't have the codec specific data.
                 * We will add it when we receive the first frame.
                 */
                streamConfigs.associateWith { numOfStreams++ }
            } finally {
                setState(State.CONFIGURED)
            }
        }
    }

    override suspend fun addStream(streamConfig: CodecConfig) =
        mutex.withLock {
            require(state != State.RELEASED) { "Muxer is released" }
            try {
                requireNotNull(mediaMuxer) { "MediaMuxer is not initialized" }
                /**
                 * We can't addTrack here because we don't have the codec specific data.
                 * We will add it when we receive the first frame.
                 */
                numOfStreams++
            } finally {
                setState(State.CONFIGURED)
            }
        }

    override suspend fun startStream() {
        mutex.withLock {
            when (state) {
                State.PENDING_START, State.STARTED -> {
                    return
                }

                State.RELEASED, State.PENDING_RELEASE -> {
                    throw IllegalStateException("Muxer is released")
                }

                State.PENDING_STOP, State.STOPPED, State.ERROR -> {
                    throw IllegalStateException("Muxer is stopping or stopped")
                }

                State.IDLE -> {
                    throw IllegalStateException("Muxer is not configured")
                }

                State.CONFIGURED -> {
                    require(numOfStreams > 0) { "No streams added" }
                    setState(State.PENDING_START)
                }
            }
        }
    }

    override suspend fun stopStream() {
        mutex.withLock {
            when (state) {
                State.PENDING_STOP, State.STOPPED, State.ERROR, State.IDLE, State.CONFIGURED -> {
                    return
                }

                State.RELEASED, State.PENDING_RELEASE -> {
                    throw IllegalStateException("Muxer is released")
                }

                State.PENDING_START -> {
                    setState(State.CONFIGURED)
                }

                State.STARTED -> {
                    setState(State.PENDING_STOP)
                    try {
                        mediaMuxer?.stop()
                    } catch (_: IllegalStateException) {
                    } finally {
                        setState(State.STOPPED)
                    }
                }
            }
        }
    }

    private suspend fun closeUnsafe() {
        try {
            try {
                fileDescriptor?.close()
            } catch (_: Throwable) {
            } finally {
                fileDescriptor = null
            }

            mediaMuxer?.release()
        } catch (_: Throwable) {
        } finally {
            numOfStreams = 0
            streamIdToTrackId.clear()
            mediaMuxer = null
            _isOpenFlow.emit(false)
        }
    }

    override suspend fun close() {
        mutex.withLock {
            closeUnsafe()
            setState(State.IDLE)
        }
    }

    override fun release() {
        runBlocking {
            mutex.withLock {
                setState(State.PENDING_RELEASE)
                closeUnsafe()
                setState(State.RELEASED)
            }
        }
    }

    private fun addTrack(mediaMuxer: MediaMuxer, streamId: Int, format: MediaFormat) {
        if (streamIdToTrackId[streamId] == null) {
            streamIdToTrackId[streamId] = mediaMuxer.addTrack(format)
        }
    }

    private fun setState(state: State) {
        if (state == this.state) {
            return
        }
        Logger.d(TAG, "Transitioning muxer internal state: ${this.state} --> $state")
        this.state = state
    }

    object Mp4EndpointInfo : IEndpoint.IEndpointInfo {
        override val audio by lazy {
            object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
                override val supportedEncoders by lazy {
                    listOf(
                        MediaFormat.MIMETYPE_AUDIO_AAC,
                        MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                        MediaFormat.MIMETYPE_AUDIO_AMR_WB
                    )
                }
                override val supportedSampleRates = null
                override val supportedByteFormats = null
            }
        }

        override val video by lazy {
            object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
                override val supportedEncoders by lazy {
                    mutableListOf(
                        MediaFormat.MIMETYPE_VIDEO_H263,
                        MediaFormat.MIMETYPE_VIDEO_AVC,
                        MediaFormat.MIMETYPE_VIDEO_MPEG4,
                        MediaFormat.MIMETYPE_VIDEO_APV
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            add(MediaFormat.MIMETYPE_VIDEO_HEVC)
                        }
                    }
                }
            }
        }
    }

    object WebMEndpointInfo : IEndpoint.IEndpointInfo {
        override val audio by lazy {
            object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
                override val supportedEncoders by lazy {
                    listOf(
                        MediaFormat.MIMETYPE_AUDIO_VORBIS, MediaFormat.MIMETYPE_AUDIO_OPUS
                    )
                }
                override val supportedSampleRates = null
                override val supportedByteFormats = null
            }
        }

        override val video by lazy {
            object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
                override val supportedEncoders by lazy {
                    mutableListOf(
                        MediaFormat.MIMETYPE_VIDEO_VP8
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            add(MediaFormat.MIMETYPE_VIDEO_VP9)
                        }
                    }
                }
            }
        }
    }

    object ThreeGPEndpointInfo : IEndpoint.IEndpointInfo {
        override val audio by lazy {
            object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
                override val supportedEncoders by lazy {
                    listOf(
                        MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                        MediaFormat.MIMETYPE_AUDIO_AMR_WB,
                        MediaFormat.MIMETYPE_AUDIO_AAC,
                    )
                }
                override val supportedSampleRates = null
                override val supportedByteFormats = null
            }
        }

        override val video by lazy {
            object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
                override val supportedEncoders by lazy {
                    mutableListOf(
                        MediaFormat.MIMETYPE_VIDEO_H263, MediaFormat.MIMETYPE_VIDEO_AVC
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            add(MediaFormat.MIMETYPE_VIDEO_VP9)
                            add(MediaFormat.MIMETYPE_VIDEO_HEVC)
                        }
                    }
                }
            }
        }
    }

    object OggEndpointInfo : IEndpoint.IEndpointInfo {
        override val audio by lazy {
            object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
                override val supportedEncoders by lazy {
                    listOf(
                        MediaFormat.MIMETYPE_AUDIO_VORBIS, MediaFormat.MIMETYPE_AUDIO_OPUS
                    )
                }
                override val supportedSampleRates = null
                override val supportedByteFormats = null
            }
        }

        override val video by lazy {
            object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
                override val supportedEncoders by lazy { emptyList<String>() }
            }
        }
    }

    private val MediaContainerType.outputFormat: Int
        get() = when (this) {
            MediaContainerType.MP4 -> OutputFormat.MUXER_OUTPUT_MPEG_4
            MediaContainerType.THREEGP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OutputFormat.MUXER_OUTPUT_3GPP
            } else {
                throw IllegalArgumentException("Unsupported container type for API < 26: $this")
            }

            MediaContainerType.WEBM -> OutputFormat.MUXER_OUTPUT_WEBM
            MediaContainerType.OGG -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputFormat.MUXER_OUTPUT_OGG
            } else {
                throw IllegalArgumentException("Unsupported container type for API < 29: $this")
            }

            else -> throw IllegalArgumentException("Unsupported container type: $this")
        }

    companion object {
        private const val TAG = "MediaMuxerEndpoint"

        private fun getInfo(descriptor: MediaDescriptor) = getInfo(descriptor.type.containerType)

        private fun getInfo(type: MediaDescriptor.Type) = getInfo(type.containerType)

        private fun getInfo(containerType: MediaContainerType) = when (containerType) {
            MediaContainerType.MP4 -> Mp4EndpointInfo
            MediaContainerType.THREEGP -> ThreeGPEndpointInfo
            MediaContainerType.WEBM -> WebMEndpointInfo
            MediaContainerType.OGG -> OggEndpointInfo
            else -> throw IllegalArgumentException("Unsupported container type: $containerType")
        }
    }

    private enum class State {
        IDLE,

        CONFIGURED,

        STARTED,

        STOPPED,

        PENDING_START,

        PENDING_STOP,

        PENDING_RELEASE,

        ERROR,

        RELEASED;

        val isRunning: Boolean
            get() = this == STARTED
    }
}

/**
 * Factory for [MediaMuxerEndpoint].
 */
class MediaMuxerEndpointFactory : IEndpointInternal.Factory {
    override fun create(
        context: Context, dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal {
        return MediaMuxerEndpoint(context, dispatcherProvider.io)
    }
}