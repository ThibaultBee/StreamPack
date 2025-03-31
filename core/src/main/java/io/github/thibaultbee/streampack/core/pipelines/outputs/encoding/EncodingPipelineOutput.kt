/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.pipelines.outputs.encoding

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.EncoderMode
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.AudioEncoderConfig
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.MediaCodecEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.VideoEncoderConfig
import io.github.thibaultbee.streampack.core.elements.encoders.rotateFromNaturalOrientation
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.elements.utils.extensions.sourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.mapState
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioSyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceWithSize
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * An implementation of [IEncodingPipelineOutputInternal] that manages encoding and endpoint.
 *
 * @param context The application context
 * @param hasAudio whether the output has audio.
 * @param hasVideo whether the output has video.
 * @param endpointFactory The endpoint factory implementation
 * @param defaultRotation The default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param coroutineDispatcher The coroutine dispatcher to use. By default, it is [Dispatchers.Default]
 */
internal class EncodingPipelineOutput(
    private val context: Context,
    override val hasAudio: Boolean = true,
    override val hasVideo: Boolean = true,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IConfigurableEncodingPipelineOutput, IEncodingPipelineOutputInternal,
    IVideoSurfacePipelineOutputInternal, IAudioSyncPipelineOutputInternal {
    /**
     * Mutex to avoid concurrent start/stop operations.
     */
    private val startStopStreamMutex = Mutex()

    /**
     * Mutex to avoid concurrent audio configuration operations.
     */
    private val audioConfigurationMutex = Mutex()

    /**
     * Mutex to avoid concurrent video configuration operations.
     */
    private val videoConfigurationMutex = Mutex()

    private var bitrateRegulatorController: IBitrateRegulatorController? = null

    private var audioStreamId: Int? = null
    private var videoStreamId: Int? = null

    // INPUTS
    private val _surfaceFlow = MutableStateFlow<SurfaceWithSize?>(null)

    /**
     * The surface used to encode video.
     *
     * It is emitted when the video configuration changed and after a [stopStream] (async).
     */
    override val surfaceFlow = _surfaceFlow.asStateFlow()

    // ENCODERS
    private var audioEncoderInternal: IEncoderInternal? = null
    override val audioEncoder: IEncoder?
        get() = audioEncoderInternal

    private var videoEncoderInternal: IEncoderInternal? = null
    override val videoEncoder: IEncoder?
        get() = videoEncoderInternal

    // ENDPOINT
    private val endpointInternal: IEndpointInternal = endpointFactory.create(context)
    override val endpoint: IEndpoint
        get() = endpointInternal

    /**
     * Keep the target rotation if it can't be applied immediately.
     * It will be applied when the stream is stopped.
     */
    @RotationValue
    private var pendingTargetRotation: Int? = null

    /**
     * The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    @RotationValue
    private var _targetRotation = defaultRotation

    override var targetRotation: Int
        @RotationValue get() = _targetRotation
        set(@RotationValue value) {
            if (!hasVideo) {
                Logger.w(TAG, "Video is not enabled")
                return
            }
            if (isStreaming) {
                Logger.w(TAG, "Can't change rotation to $value while streaming")
                pendingTargetRotation = value
                return
            }

            setTargetRotationInternal(value)
        }

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow = _throwableFlow.asStateFlow()

    override val isOpenFlow: StateFlow<Boolean> = endpointInternal.isOpenFlow

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    /**
     * Called when audio configuration is set.
     * The purpose is to validate and apply audio configuration to the source.
     */
    override var audioConfigEventListener: IConfigurableAudioPipelineOutputInternal.Listener? = null

    /**
     * Called when video configuration is set.
     * The purpose is to validate and apply video configuration to the source.
     */
    override var videoConfigEventListener: IConfigurableVideoPipelineOutputInternal.Listener? = null

    /**
     * Called when stream starts.
     * It is called after the endpoint is started.
     * The purpose is to start any other required components.
     */
    override var streamEventListener: IPipelineEventOutputInternal.Listener? = null

    /**
     * Manages error on stream.
     * Stops only stream.
     *
     * @param t triggered [Throwable]
     */
    private fun onInternalError(t: Throwable) {
        try {
            runBlocking {
                if (isStreaming) {
                    stopStream()
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "onStreamError: Can't stop stream", t)
        } finally {
            Logger.e(TAG, "onStreamError: ${t.message}", t)
            _throwableFlow.tryEmit(t)
        }
    }

    private val audioEncoderListener = object : IEncoderInternal.IListener {
        override fun onError(t: Throwable) {
            onInternalError(t)
        }

        override fun onOutputFrame(frame: Frame) {
            audioStreamId?.let {
                runBlocking {
                    this@EncodingPipelineOutput.endpointInternal.write(frame, it)
                }
            } ?: Logger.w(TAG, "Audio frame received but audio stream is not set")
        }
    }

    private val videoEncoderListener = object : IEncoderInternal.IListener {
        override fun onError(t: Throwable) {
            onInternalError(t)
        }

        override fun onOutputFrame(frame: Frame) {
            videoStreamId?.let {
                runBlocking {
                    this@EncodingPipelineOutput.endpointInternal.write(frame, it)
                }
            } ?: Logger.w(TAG, "Video frame received but video stream is not set")
        }
    }

    private val _audioCodecConfigFlow = MutableStateFlow<AudioCodecConfig?>(null)
    override val audioCodecConfigFlow = _audioCodecConfigFlow.asStateFlow()
    override val audioSourceConfigFlow = audioCodecConfigFlow.mapState { it?.sourceConfig }

    private val audioCodecConfig: AudioCodecConfig?
        get() = audioCodecConfigFlow.value

    override suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig) {
        require(hasAudio) { "Audio is not enabled" }
        withContext(coroutineDispatcher) {
            audioConfigurationMutex.withLock {
                setAudioCodecConfigInternal(audioCodecConfig)
            }
        }
    }

    override fun queueAudioFrame(frame: RawFrame) {
        val encoder = requireNotNull(audioEncoderInternal) { "Audio is not configured" }
        val input = encoder.input as IEncoderInternal.ISyncByteBufferInput
        input.queueInputFrame(
            frame
        )
    }

    private suspend fun setAudioCodecConfigInternal(audioCodecConfig: AudioCodecConfig) {
        require(!isStreaming) { "Can't change audio configuration while streaming" }

        if (this.audioCodecConfig == audioCodecConfig) {
            Logger.i(TAG, "Audio configuration is the same, skipping configuration")
            return
        }

        audioConfigEventListener?.onSetAudioSourceConfig(audioCodecConfig.sourceConfig)

        applyAudioCodecConfig(audioCodecConfig)
        _audioCodecConfigFlow.emit(audioCodecConfig)
    }

    private fun applyAudioCodecConfig(audioConfig: AudioCodecConfig) {
        try {
            audioEncoderInternal?.release()
            audioEncoderInternal = MediaCodecEncoder(
                AudioEncoderConfig(
                    audioConfig,
                    EncoderMode.SYNC
                ), listener = audioEncoderListener
            ).apply {
                configure()
            }
        } catch (t: Throwable) {
            audioEncoderInternal?.release()
            audioEncoderInternal = null
            throw t
        }
    }

    private val _videoCodecConfigFlow = MutableStateFlow<VideoCodecConfig?>(null)
    override val videoCodecConfigFlow = _videoCodecConfigFlow.asStateFlow()
    override val videoSourceConfigFlow: StateFlow<VideoSourceConfig?> =
        videoCodecConfigFlow.mapState { it?.sourceConfig }

    private val videoCodecConfig: VideoCodecConfig?
        get() = videoCodecConfigFlow.value

    override suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig) {
        require(hasVideo) { "Video is not enabled" }
        withContext(coroutineDispatcher) {
            videoConfigurationMutex.withLock {
                setVideoCodecConfigInternal(videoCodecConfig)
            }
        }
    }

    private suspend fun setVideoCodecConfigInternal(videoCodecConfig: VideoCodecConfig) {
        require(!isStreaming) { "Can't change video configuration while streaming" }

        if (this.videoCodecConfig == videoCodecConfig) {
            Logger.i(TAG, "Video configuration is the same, skipping configuration")
            return
        }

        videoConfigEventListener?.onSetVideoSourceConfig(videoCodecConfig.sourceConfig)

        applyVideoCodecConfig(videoCodecConfig)

        _videoCodecConfigFlow.emit(videoCodecConfig)
    }

    private fun applyVideoCodecConfig(videoConfig: VideoCodecConfig) {
        try {
            videoEncoderInternal = buildAndConfigureVideoEncoder(
                videoConfig, targetRotation
            )
        } catch (t: Throwable) {
            videoEncoderInternal?.release()
            videoEncoderInternal = null
            throw t
        }
    }

    private fun buildAndConfigureVideoEncoder(
        videoConfig: VideoCodecConfig, @RotationValue targetRotation: Int
    ): IEncoderInternal {
        val rotatedVideoConfig = videoConfig.rotateFromNaturalOrientation(context, targetRotation)

        // Release codec instance
        videoEncoderInternal?.let { encoder ->
            val input = encoder.input
            if (input is MediaCodecEncoder.SurfaceInput) {
                _surfaceFlow.tryEmit(null)
            }
            encoder.release()
        }

        // Prepare new codec instance
        return buildVideoEncoder(rotatedVideoConfig).apply {
            configure()
        }
    }

    private fun buildVideoEncoder(videoConfig: VideoCodecConfig): IEncoderInternal {
        val videoEncoder = MediaCodecEncoder(
            VideoEncoderConfig(
                videoConfig, EncoderMode.SURFACE
            ), listener = videoEncoderListener
        )

        when (videoEncoder.input) {
            is MediaCodecEncoder.SurfaceInput -> {
                videoEncoder.input.listener =
                    object : IEncoderInternal.ISurfaceInput.OnSurfaceUpdateListener {
                        override fun onSurfaceUpdated(surface: Surface) {
                            _surfaceFlow.tryEmit(SurfaceWithSize(surface, videoConfig.resolution))
                        }
                    }
            }

            else -> {
                throw UnsupportedOperationException("Unknown input type: ${videoEncoder.input}")
            }
        }

        return videoEncoder
    }

    /**
     * Opens the output endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    override suspend fun open(descriptor: MediaDescriptor) = withContext(coroutineDispatcher) {
        endpointInternal.open(descriptor)
    }

    /**
     * Closes the streamer endpoint.
     */
    override suspend fun close() = withContext(coroutineDispatcher) {
        if (!isOpenFlow.value) {
            Logger.i(TAG, "Endpoint is already closed")
            return@withContext
        }
        startStopStreamMutex.withLock {
            stopStreamImpl()
        }
        endpointInternal.close()
    }

    /**
     * Starts audio and/or video streams without a concurrent lock.
     *
     * @see [stopStream]
     */
    private suspend fun startStreamInternal() {
        if (isStreaming) {
            Logger.i(TAG, "Stream is already running")
            return
        }
        require(isOpenFlow.value) { "Endpoint must be opened before starting stream" }
        require(hasAudio || hasVideo) { "At least one of audio or video must be set" }
        if (hasAudio) {
            requireNotNull(audioCodecConfig) { "Audio configuration must be set" }
        }
        if (hasVideo) {
            requireNotNull(videoCodecConfig) { "Video configuration must be set" }
        }

        try {
            _isStreamingFlow.emit(true)

            val streams = mutableListOf<CodecConfig>()
            val orientedVideoConfig = videoCodecConfig?.let {
                /**
                 * We need to get oriented size for the muxer.
                 * For example, the [FlvMuxer] `onMetaData` event needs to know the oriented size.
                 */
                it.rotateFromNaturalOrientation(context, targetRotation).apply {
                    streams.add(this)
                }
            }

            audioCodecConfig?.let {
                streams.add(it)
            }

            val streamsIdMap = endpointInternal.addStreams(streams)
            orientedVideoConfig?.let {
                videoStreamId = streamsIdMap[it]
            }
            audioCodecConfig?.let { audioStreamId = streamsIdMap[it] }

            endpointInternal.startStream()

            audioEncoderInternal?.startStream()
            videoEncoderInternal?.startStream()

            streamEventListener?.onStartStream()

            bitrateRegulatorController?.start()
        } catch (t: Throwable) {
            stopStreamImpl()
            throw t
        }
    }

    /**
     * Starts audio and/or video streams.
     *
     * Before starting the stream, the endpoint must be opened with [open] and the audio and/or
     * video configuration must be set.
     *
     * @see [stopStream]
     */
    override suspend fun startStream() = withContext(coroutineDispatcher) {
        audioConfigurationMutex.lock()
        videoConfigurationMutex.lock()

        try {
            startStopStreamMutex.withLock {
                startStreamInternal()
            }
        } finally {
            audioConfigurationMutex.unlock()
            videoConfigurationMutex.unlock()
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
    override suspend fun stopStream() = withContext(coroutineDispatcher) {
        audioConfigurationMutex.lock()
        videoConfigurationMutex.lock()

        try {
            startStopStreamMutex.withLock {
                stopStreamImpl()
            }
        } finally {
            audioConfigurationMutex.unlock()
            videoConfigurationMutex.unlock()
        }
    }

    /**
     * Stops audio/video and reset stream implementation.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamImpl() {
        if (!isStreaming) {
            Logger.i(TAG, "Stream is already stopped")
            return
        }

        streamEventListener?.onStopStream()

        stopStreamElements()

        try {
            audioEncoderInternal?.reset()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't reset audio encoder: ${t.message}")
        }
        try {
            resetVideoEncoder()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't reset video encoder: ${t.message}")
        }

        _isStreamingFlow.emit(false)
    }

    private suspend fun resetVideoEncoder() {
        _surfaceFlow.emit(null)

        val previousVideoEncoder = videoEncoderInternal
        pendingTargetRotation?.let {
            setTargetRotationInternal(it)
        }
        pendingTargetRotation = null

        // Only reset if the encoder is the same. Otherwise, it is already configured.
        if (previousVideoEncoder == videoEncoderInternal) {
            videoEncoderInternal?.reset()
        }
    }

    /**
     * Stops audio/video stream implementation without a concurrent lock.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamElements() {
        try {
            bitrateRegulatorController?.stop()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't stop bitrate regulator controller: ${t.message}")
        }

        // Encoders
        try {
            audioEncoderInternal?.stopStream()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't stop audio encoder: ${t.message}")
        }
        try {
            videoEncoderInternal?.stopStream()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't stop video encoder: ${t.message}")
        }

        // Endpoint
        try {
            endpointInternal.stopStream()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't stop endpoint: ${t.message}")
        }
    }

    /**
     * Releases endpoint and encoders.
     */
    override suspend fun release() = withContext(coroutineDispatcher) {
        _isStreamingFlow.emit(false)

        // Encoders
        audioConfigurationMutex.withLock {
            try {
                audioEncoderInternal?.release()
            } catch (t: Throwable) {
                Logger.w(TAG, "Can't release audio encoder: ${t.message}")
            } finally {
                audioEncoderInternal = null
            }
        }

        videoConfigurationMutex.withLock {
            try {
                videoEncoderInternal?.release()
            } catch (t: Throwable) {
                Logger.w(TAG, "Can't release video encoder: ${t.message}")
            } finally {
                _surfaceFlow.tryEmit(null)
                videoEncoderInternal = null
            }
        }

        // Endpoint
        try {
            endpointInternal.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't release endpoint: ${t.message}")
        }
    }

    /**
     * Adds a bitrate regulator controller.
     *
     * Limitation: it is only available for SRT for now.
     */
    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) {
        bitrateRegulatorController?.stop()
        bitrateRegulatorController = controllerFactory.newBitrateRegulatorController(this).apply {
            if (isStreaming) {
                this.start()
            }
            Logger.d(
                TAG, "Bitrate regulator controller added: ${this.javaClass.simpleName}"
            )
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

    private fun setTargetRotationInternal(@RotationValue newTargetRotation: Int) {
        if (shouldUpdateRotation(newTargetRotation)) {
            updateVideoEncoderForTransformation()
        }
    }

    private fun updateVideoEncoderForTransformation() {
        val videoConfig = videoCodecConfig
        if (videoConfig != null) {
            runBlocking {
                videoConfigurationMutex.withLock {
                    applyVideoCodecConfig(videoConfig)
                }
            }
        }
    }

    /**
     * @return true if the target rotation has changed
     */
    private fun shouldUpdateRotation(@RotationValue newTargetRotation: Int): Boolean {
        return if (targetRotation != newTargetRotation) {
            _targetRotation = newTargetRotation
            true
        } else {
            false
        }
    }

    companion object {
        private const val TAG = "EncodingPipelineOutput"
    }
}
