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
package io.github.thibaultbee.streampack.internal.endpoints

import android.content.Context
import android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.internal.data.Frame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import java.security.InvalidParameterException

/**
 * An [IEndpoint] implementation of the [MediaMuxer].
 */
class MediaMuxerEndpoint(
    private val context: Context,
) : IEndpoint {
    private var mediaMuxer: MediaMuxer? = null
    private var containerType: MediaContainerType? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    private var isStarted = false

    /**
     * Map streamId to MediaMuxer trackId
     */
    private val streamIdToTrackId = mutableMapOf<Int, Int>()
    private var numOfStreams = 0

    override val info: IPublicEndpoint.IEndpointInfo
        get() = containerType?.let { Companion.getInfo(it) }
            ?: throw IllegalStateException("Endpoint is not opened")

    override fun getInfo(type: MediaDescriptor.Type) = Companion.getInfo(type)

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened

    override suspend fun open(descriptor: MediaDescriptor) {
        require(!isOpened.value) { "Endpoint is already opened" }
        require((descriptor.type.sinkType == MediaSinkType.FILE) || (descriptor.type.sinkType == MediaSinkType.CONTENT)) { "MediaDescriptor must have a path" }
        val containerType = descriptor.type.containerType
        require(
            (containerType == MediaContainerType.MP4) ||
                    (containerType == MediaContainerType.THREEGP) ||
                    (containerType == MediaContainerType.WEBM) ||
                    (containerType == MediaContainerType.OGG)
        ) {
            "Unsupported container type: $containerType"
        }
        this.containerType = containerType

        try {
            when (descriptor.type.sinkType) {
                MediaSinkType.FILE -> {
                    val path = descriptor.uri.path
                        ?: throw IllegalStateException("Could not get path from uri: ${descriptor.uri}")
                    mediaMuxer =
                        MediaMuxer(path, containerType.outputFormat)
                }

                MediaSinkType.CONTENT -> {
                    fileDescriptor =
                        context.contentResolver.openFileDescriptor(
                            descriptor.uri,
                            "w"
                        )?.apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mediaMuxer =
                                    MediaMuxer(
                                        this.fileDescriptor,
                                        containerType.outputFormat
                                    )
                            } else {
                                throw IllegalStateException("Using content sink for API < 26 is not supported. Use file sink instead.")
                            }
                        }
                            ?: throw IllegalStateException("Could not open file descriptor for uri: ${descriptor.uri}")
                }

                else -> throw InvalidParameterException("Unsupported sink type: ${descriptor.type.sinkType}")
            }
        } catch (e: Exception) {
            this.containerType = null
            mediaMuxer?.release()
            mediaMuxer = null
            fileDescriptor?.close()
            fileDescriptor = null
            throw e
        }

        _isOpened.emit(true)
    }

    override suspend fun write(frame: Frame, streamPid: Int) {
        mediaMuxer?.let {
            if (streamIdToTrackId.size < numOfStreams) {
                addTrack(it, streamPid, frame.format)
            }
            if (streamIdToTrackId.size == numOfStreams) {
                synchronized(this) {
                    if (!isStarted) {
                        it.start()
                        isStarted = true
                    }
                }

                val trackId = streamIdToTrackId[streamPid]
                    ?: throw IllegalStateException("Could not find trackId for streamPid $streamPid: ${frame.format}")
                val info = BufferInfo().apply {
                    set(
                        0,
                        frame.buffer.remaining(),
                        frame.pts,
                        if (frame.isKeyFrame) BUFFER_FLAG_KEY_FRAME else 0
                    )
                }
                return it.writeSampleData(trackId, frame.buffer, info)
            } else {
                return
            }
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    private fun addTrack(mediaMuxer: MediaMuxer, streamId: Int, format: MediaFormat) {
        if (streamIdToTrackId[streamId] == null) {
            streamIdToTrackId[streamId] = mediaMuxer.addTrack(format)
        }
    }

    override fun addStreams(streamConfigs: List<Config>): Map<Config, Int> {
        mediaMuxer?.let {
            /**
             * We can't addTrack here because we don't have the codec specific data.
             * We will add it when we receive the first frame.
             */
            return streamConfigs.associateWith { numOfStreams++ }
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    override fun addStream(streamConfig: Config): Int {
        mediaMuxer?.let {
            /**
             * We can't addTrack here because we don't have the codec specific data.
             * We will add it when we receive the first frame.
             */
            return numOfStreams++
        }
        throw IllegalStateException("MediaMuxer is not initialized")
    }

    override suspend fun startStream() {
        require(mediaMuxer != null) { "MediaMuxer is not initialized" }
        /**
         * [MediaMuxer.start] is called when we called addTrack for each stream.
         */
    }

    override suspend fun stopStream() {
        try {
            mediaMuxer?.stop()
        } catch (_: IllegalStateException) {
        }

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {
        } finally {
            fileDescriptor = null
        }

        numOfStreams = 0
        streamIdToTrackId.clear()
        isStarted = false
    }

    override suspend fun close() {
        stopStream()
        try {
            mediaMuxer?.release()
        } catch (e: Exception) {
            // MediaMuxer is already released
        } finally {
            mediaMuxer = null
            _isOpened.emit(false)
        }
    }

    override fun release() {
        runBlocking {
            stopStream()
            close()
        }
    }

    object Mp4EndpointInfo : IPublicEndpoint.IEndpointInfo {
        override val audio = object : IPublicEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = listOf(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                MediaFormat.MIMETYPE_AUDIO_AMR_WB
            )
            override val supportedSampleRates = null
            override val supportedByteFormats = null
        }

        override val video = object : IPublicEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = run {
                mutableListOf(
                    MediaFormat.MIMETYPE_VIDEO_H263,
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    MediaFormat.MIMETYPE_VIDEO_MPEG4
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        add(MediaFormat.MIMETYPE_VIDEO_HEVC)
                    }
                }
            }
        }
    }

    object WebMEndpointInfo : IPublicEndpoint.IEndpointInfo {
        override val audio = object : IPublicEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = listOf(
                MediaFormat.MIMETYPE_AUDIO_VORBIS,
                MediaFormat.MIMETYPE_AUDIO_OPUS
            )
            override val supportedSampleRates = null
            override val supportedByteFormats = null
        }

        override val video = object : IPublicEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = run {
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

    object ThreeGPEndpointInfo : IPublicEndpoint.IEndpointInfo {
        override val audio = object : IPublicEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = listOf(
                MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                MediaFormat.MIMETYPE_AUDIO_AMR_WB,
                MediaFormat.MIMETYPE_AUDIO_AAC,
            )
            override val supportedSampleRates = null
            override val supportedByteFormats = null
        }

        override val video = object : IPublicEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = run {
                mutableListOf(
                    MediaFormat.MIMETYPE_VIDEO_H263,
                    MediaFormat.MIMETYPE_VIDEO_AVC
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        add(MediaFormat.MIMETYPE_VIDEO_VP9)
                        add(MediaFormat.MIMETYPE_VIDEO_HEVC)
                    }
                }
            }
        }
    }

    object OggEndpointInfo : IPublicEndpoint.IEndpointInfo {
        override val audio = object : IPublicEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = listOf(
                MediaFormat.MIMETYPE_AUDIO_VORBIS,
                MediaFormat.MIMETYPE_AUDIO_OPUS
            )
            override val supportedSampleRates = null
            override val supportedByteFormats = null
        }

        override val video = object : IPublicEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = emptyList<String>()
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
        private fun getInfo(descriptor: MediaDescriptor) = getInfo(descriptor.type.containerType)

        private fun getInfo(type: MediaDescriptor.Type) = getInfo(type.containerType)

        private fun getInfo(containerType: MediaContainerType) =
            when (containerType) {
                MediaContainerType.MP4 -> Mp4EndpointInfo
                MediaContainerType.THREEGP -> ThreeGPEndpointInfo
                MediaContainerType.WEBM -> WebMEndpointInfo
                MediaContainerType.OGG -> OggEndpointInfo
                else -> throw IllegalArgumentException("Unsupported container type: $containerType")
            }
    }
}