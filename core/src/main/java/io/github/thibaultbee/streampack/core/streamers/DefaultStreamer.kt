/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamers

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.error.StreamPackError
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.encoders.IEncoder
import io.github.thibaultbee.streampack.core.internal.encoders.IPublicEncoder
import io.github.thibaultbee.streampack.core.internal.encoders.mediacodec.AudioEncoderConfig
import io.github.thibaultbee.streampack.core.internal.encoders.mediacodec.MediaCodecEncoder
import io.github.thibaultbee.streampack.core.internal.encoders.mediacodec.VideoEncoderConfig
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.core.internal.gl.CodecSurface
import io.github.thibaultbee.streampack.core.internal.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.internal.sources.audio.IPublicAudioSource
import io.github.thibaultbee.streampack.core.internal.sources.video.IPublicVideoSource
import io.github.thibaultbee.streampack.core.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.StreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Base class of all streamers.
 *
 * @param context the application context
 * @param internalVideoSource the video source
 * @param internalAudioSource the audio source
 * @param internalEndpoint the [IEndpoint] implementation
 * @param dispatcher the [CoroutineDispatcher] to execute suspendable methods. For test only. Do not change.
 */
open class DefaultStreamer(
    private val context: Context,
    protected val internalAudioSource: IAudioSource?,
    protected val internalVideoSource: IVideoSource?,
    protected val internalEndpoint: IEndpoint = DynamicEndpoint(context),
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()
) : ICoroutineStreamer {
    private val _throwable = MutableStateFlow<Throwable?>(null)
    override val throwable: StateFlow<Throwable?> = _throwable

    private var audioStreamId: Int? = null
    private var videoStreamId: Int? = null

    private var bitrateRegulatorController: IBitrateRegulatorController? = null

    // Keep video configuration
    private var videoConfig: VideoConfig? = null
    private var audioConfig: AudioConfig? = null

    private val sourceOrientationProvider = internalVideoSource?.orientationProvider

    private val audioEncoderListener =
        object : IEncoder.IListener {
            override fun onError(e: Exception) {
                onStreamError(e)
            }

            override fun onOutputFrame(frame: Frame) {
                audioStreamId?.let {
                    runBlocking {
                        this@DefaultStreamer.internalEndpoint.write(frame, it)
                    }
                }
            }
        }

    private val videoEncoderListener =
        object : IEncoder.IListener {
            override fun onError(e: Exception) {
                onStreamError(e)
            }

            override fun onOutputFrame(frame: Frame) {
                videoStreamId?.let {
                    frame.pts += internalVideoSource!!.timestampOffset
                    frame.dts = if (frame.dts != null) {
                        frame.dts!! + internalVideoSource.timestampOffset
                    } else {
                        null
                    }
                    runBlocking {
                        this@DefaultStreamer.internalEndpoint.write(frame, it)
                    }
                }
            }
        }

    /**
     * Manages error on stream.
     * Stops only stream.
     *
     * @param t triggered [Throwable]
     */
    protected fun onStreamError(t: Throwable) {
        try {
            runBlocking {
                stopStream()
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "onStreamError: Can't stop stream", t)
        } finally {
            Logger.e(TAG, "onStreamError: ${t.message}", t)
            _throwable.tryEmit(t)
        }
    }

    // SOURCES

    /**
     * The audio source.
     * It allows advanced audio settings.
     */
    override val audioSource: IPublicAudioSource?
        get() = internalAudioSource

    /**
     * The video source.
     * It allows advanced video settings.
     */
    override val videoSource: IPublicVideoSource?
        get() = internalVideoSource

    // ENCODERS

    private var internalAudioEncoder: MediaCodecEncoder? = null

    /**
     * The audio encoder.
     * Only valid when audio has been [configure]. It is null after [release].
     */
    override val audioEncoder: IPublicEncoder?
        get() = internalAudioEncoder

    private var internalVideoEncoder: MediaCodecEncoder? = null

    /**
     * The video encoder.
     * Only valid when audio has been [configure]. It is null after [release].
     */
    override val videoEncoder: IPublicEncoder?
        get() = internalVideoEncoder


    protected val codecSurface =
        if (internalVideoSource?.hasOutputSurface == true) CodecSurface(sourceOrientationProvider) else null

    // ENDPOINT

    override val endpoint: IPublicEndpoint
        get() = internalEndpoint

    override val isOpen: StateFlow<Boolean>
        get() = internalEndpoint.isOpen


    private val _isStreaming = MutableStateFlow(false)
    override val isStreaming: StateFlow<Boolean> = _isStreaming

    /**
     * Whether the streamer has audio.
     */
    val hasAudio = internalAudioSource != null

    /**
     * Whether the streamer has video.
     */
    val hasVideo = internalVideoSource != null

    /**
     * Gets configuration information.
     *
     * Could throw an exception if the endpoint needs to infer the configuration from the
     * [MediaDescriptor].
     * In this case, prefer using [getInfo] with the [MediaDescriptor] used in [open].
     */
    override val info: IConfigurationInfo
        get() = StreamerConfigurationInfo(endpoint.info)

    /**
     * Gets configuration information from [MediaDescriptor].
     *
     * If the endpoint is not [DynamicEndpoint], [descriptor] is unused as the endpoint type is
     * already known.
     *
     * @param descriptor the media descriptor
     */
    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        val endpointInfo = try {
            endpoint.info
        } catch (_: Exception) {
            endpoint.getInfo(descriptor)
        }
        return StreamerConfigurationInfo(endpointInfo)
    }

    /**
     * Configures audio settings.
     * It is the first method to call after a [DefaultStreamer] instantiation.
     * It must be call when both stream and audio capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun configure(audioConfig: AudioConfig) {
        require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
        requireNotNull(internalAudioSource) { "Audio source must not be null" }

        this.audioConfig = audioConfig

        try {
            internalAudioSource.configure(audioConfig)

            internalAudioEncoder?.release()
            internalAudioEncoder =
                MediaCodecEncoder(
                    AudioEncoderConfig(
                        audioConfig
                    ),
                    listener = audioEncoderListener
                ).apply {
                    if (input is MediaCodecEncoder.ByteBufferInput) {
                        input.listener =
                            object :
                                IEncoder.IByteBufferInput.OnFrameRequestedListener {
                                override fun onFrameRequested(buffer: ByteBuffer): Frame {
                                    return internalAudioSource.getFrame(buffer)
                                }
                            }
                    } else {
                        throw UnsupportedOperationException("Audio encoder only support ByteBuffer mode")
                    }
                    configure()
                }
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    private fun buildVideoEncoder(
        videoConfig: VideoConfig,
        videoSource: IVideoSource
    ): MediaCodecEncoder {
        val videoEncoder = MediaCodecEncoder(
            VideoEncoderConfig(
                videoConfig,
                videoSource.hasOutputSurface,
                videoSource.orientationProvider
            ), listener = videoEncoderListener
        )

        when (videoEncoder.input) {
            is MediaCodecEncoder.SurfaceInput -> {
                codecSurface!!.useHighBitDepth = videoConfig.isHdr
                videoEncoder.input.listener =
                    object :
                        IEncoder.ISurfaceInput.OnSurfaceUpdateListener {
                        override fun onSurfaceUpdated(surface: Surface) {
                            Logger.d(TAG, "Updating with new encoder surface input")
                            codecSurface.outputSurface = surface
                            videoSource.outputSurface = codecSurface.input
                        }
                    }
            }

            is MediaCodecEncoder.ByteBufferInput -> {
                videoEncoder.input.listener =
                    object :
                        IEncoder.IByteBufferInput.OnFrameRequestedListener {
                        override fun onFrameRequested(buffer: ByteBuffer): Frame {
                            return videoSource.getFrame(buffer)
                        }
                    }
            }

            else -> {
                throw UnsupportedOperationException("Unknown input type")
            }
        }
        return videoEncoder
    }

    /**
     * Configures video settings.
     * It is the first method to call after a [DefaultStreamer] instantiation.
     * It must be call when both stream and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     */
    override fun configure(videoConfig: VideoConfig) {
        require(hasVideo) { "Do not need to set video as it is a audio only streamer" }
        requireNotNull(internalVideoSource) { "Video source must not be null" }

        this.videoConfig = videoConfig

        try {
            internalVideoSource.configure(videoConfig)

            internalVideoEncoder?.release()
            internalVideoEncoder = buildVideoEncoder(videoConfig, internalVideoSource).apply {
                configure()
            }
        } catch (e: Exception) {
            release()
            throw StreamPackError(e)
        }
    }

    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    override suspend fun open(descriptor: MediaDescriptor) = withContext(dispatcher) {
        internalEndpoint.open(descriptor)
    }

    /**
     * Closes the streamer endpoint.
     */
    override suspend fun close() = withContext(dispatcher) {
        stopStreamInternal()
        internalEndpoint.close()
    }

    /**
     * Starts audio/video stream.
     * Stream depends of the endpoint: Audio/video could be write to a file or send to a remote
     * device.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @see [stopStream]
     */
    override suspend fun startStream() = withContext(dispatcher) {
        require(isOpen.value) { "Endpoint must be opened before starting stream" }
        require(!isStreaming.value) { "Stream is already running" }

        try {
            val streams = mutableListOf<Config>()
            val orientedVideoConfig = if (hasVideo) {
                val videoConfig = requireNotNull(videoConfig) { "Requires video config" }
                /**
                 * If sourceOrientationProvider is not null, we need to get oriented size.
                 * For example, the [FlvMuxer] `onMetaData` event needs to know the oriented size.
                 */
                if (sourceOrientationProvider != null) {
                    val orientedSize =
                        sourceOrientationProvider.getOrientedSize(videoConfig.resolution)
                    videoConfig.copy(resolution = orientedSize)
                } else {
                    videoConfig
                }
            } else {
                null
            }
            if (orientedVideoConfig != null) {
                streams.add(orientedVideoConfig)
            }

            if (hasAudio) {
                val audioConfig = requireNotNull(audioConfig) { "Requires audio config" }
                streams.add(audioConfig)
            }

            val streamsIdMap = internalEndpoint.addStreams(streams)
            orientedVideoConfig?.let { videoStreamId = streamsIdMap[orientedVideoConfig] }
            audioConfig?.let { audioStreamId = streamsIdMap[audioConfig as Config] }

            internalEndpoint.startStream()

            internalAudioSource?.startStream()
            internalAudioEncoder?.startStream()

            internalVideoSource?.startStream()
            codecSurface?.startStream()
            internalVideoEncoder?.startStream()

            bitrateRegulatorController?.start()

            _isStreaming.emit(true)
        } catch (e: Exception) {
            stopStreamInternal()
            throw StreamPackError(e)
        }
    }

    /**
     * Stops audio/video stream.
     *
     * Internally, it resets audio and video recorders and encoders to get them ready for another
     * [startStream] session. It explains why preview could be restarted.
     *
     * @see [startStream]
     */
    override suspend fun stopStream() = withContext(dispatcher) {
        stopStreamInternal()
    }

    /**
     * Stops audio/video and reset stream implementation.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamInternal() {
        stopStreamImpl()

        internalAudioEncoder?.reset()
        internalVideoEncoder?.reset()

        _isStreaming.emit(false)
    }

    /**
     * Stops audio/video stream implementation.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamImpl() {
        bitrateRegulatorController?.stop()

        // Sources
        internalAudioSource?.stopStream()
        internalVideoSource?.stopStream()
        codecSurface?.stopStream()

        // Encoders
        internalAudioEncoder?.stopStream()
        internalVideoEncoder?.stopStream()

        // Endpoint
        internalEndpoint.stopStream()
    }

    /**
     * Releases recorders and encoders object.
     * It also stops preview if needed
     *
     * @see [configure]
     */
    override fun release() {
        // Sources
        internalAudioSource?.release()
        internalVideoSource?.release()
        codecSurface?.release()

        // Encoders
        internalAudioEncoder?.release()
        internalAudioEncoder = null
        internalVideoEncoder?.release()
        internalVideoEncoder = null

        // Endpoint
        internalEndpoint.release()
    }

    /**
     * Adds a bitrate regulator controller.
     *
     * Limitation: it is only available for SRT for now.
     */
    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) {
        bitrateRegulatorController?.stop()
        bitrateRegulatorController =
            controllerFactory.newBitrateRegulatorController(this).apply {
                if (isStreaming.value) {
                    this.start()
                }
                Logger.d(TAG, "Bitrate regulator controller added: ${this.javaClass.simpleName}")
            }

    }

    /**
     * Removes the bitrate regulator controller.
     */
    override fun removeBitrateRegulatorController() {
        bitrateRegulatorController?.stop()
        bitrateRegulatorController = null
        Logger.d(TAG, "Bitrate regulator controller removed")
    }

    companion object {
        private const val TAG = "DefaultStreamer"
    }
}