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
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
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
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.flush
import io.github.thibaultbee.streampack.core.elements.utils.extensions.sourceConfig
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider.Companion.THREAD_NAME_ENCODER_PREFIX
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider.Companion.THREAD_NAME_ENCODING_OUTPUT_PREFIX
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioCallbackPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioSyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An implementation of [IEncodingPipelineOutputInternal] that manages encoding and endpoint for
 * audio and video.
 *
 * @param context The application context
 * @param withAudio whether the output has audio.
 * @param withVideo whether the output has video.
 * @param endpointFactory The endpoint factory implementation
 * @param defaultRotation The default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param dispatcherProvider The dispatcher provider to use for coroutine dispatching
 */
internal class EncodingPipelineOutput(
    private val context: Context,
    override val withAudio: Boolean,
    override val withVideo: Boolean,
    endpointFactory: IEndpointInternal.Factory,
    @RotationValue defaultRotation: Int,
    private val dispatcherProvider: IDispatcherProvider
) : IConfigurableAudioVideoEncodingPipelineOutput, IEncodingPipelineOutputInternal,
    IVideoSurfacePipelineOutputInternal, IAudioSyncPipelineOutputInternal,
    IAudioCallbackPipelineOutputInternal {
    private val coroutineScope = CoroutineScope(dispatcherProvider.default)
    private val coroutineDispatcher = dispatcherProvider.default

    /**
     * Mutex to avoid concurrent start/stop operations.
     */
    private val mutex = Mutex()

    private val isReleaseRequested = AtomicBoolean(false)

    private var bitrateRegulatorController: IBitrateRegulatorController? = null

    private var audioStreamId: Int? = null
    private var videoStreamId: Int? = null

    // INPUTS
    override var audioFrameRequestedListener: IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener? =
        null

    private val _surfaceFlow = MutableStateFlow<SurfaceDescriptor?>(null)

    /**
     * The surface used to encode video.
     *
     * It is emitted when the video configuration changed and after a [stopStream] (async).
     */
    override val surfaceFlow = _surfaceFlow.asStateFlow()

    // ENCODERS
    private var audioInput: IEncoderInternal.ISyncByteBufferInput? = null

    private val audioOutputDispatcher =
        dispatcherProvider.createAudioDispatcher(1, THREAD_NAME_ENCODING_OUTPUT_PREFIX)
    private val audioEncoderDispatcher =
        dispatcherProvider.createAudioDispatcher(1, THREAD_NAME_ENCODER_PREFIX)

    private var audioEncoderInternal: IEncoderInternal? = null
        set(value) {
            audioInput = value?.input as? IEncoderInternal.ISyncByteBufferInput
            field = value
        }
    override val audioEncoder: IEncoder?
        get() = audioEncoderInternal

    private val videoOutputDispatcher =
        dispatcherProvider.createVideoDispatcher(1, THREAD_NAME_ENCODING_OUTPUT_PREFIX)
    private val videoEncoderDispatcher =
        dispatcherProvider.createVideoDispatcher(1, THREAD_NAME_ENCODER_PREFIX)

    private var videoEncoderInternal: IEncoderInternal? = null
    override val videoEncoder: IEncoder?
        get() = videoEncoderInternal

    // ENDPOINT
    private val endpointInternal: IEndpointInternal =
        endpointFactory.create(context, dispatcherProvider)
    override val endpoint: IEndpoint = endpointInternal

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
    override var targetRotation = defaultRotation
        private set

    /**
     * Sets the target rotation..
     *
     * It is used to set the rotation of the video encoder.
     *
     * @param rotation The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) {
        if (!withVideo) {
            Logger.w(TAG, "Video is not enabled")
            return
        }
        withContextMutex {
            if (isStreaming) {
                Logger.w(
                    TAG,
                    "Can't change rotation to $rotation while streaming. Waiting for stopStream"
                )
                pendingTargetRotation = rotation
            } else {
                setTargetRotationInternal(rotation)
            }
        }
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
                stopStream()
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

        override val outputChannel =
            Channel<FrameWithCloseable>(Channel.UNLIMITED, onUndeliveredElement = {
                it.close()
            })
    }

    private val videoEncoderListener = object : IEncoderInternal.IListener {
        override fun onError(t: Throwable) {
            onInternalError(t)
        }

        override val outputChannel =
            Channel<FrameWithCloseable>(Channel.UNLIMITED, onUndeliveredElement = {
                it.close()
            })
    }

    init {
        if (withAudio) {
            coroutineScope.launch(audioOutputDispatcher) {
                // Audio
                audioEncoderListener.outputChannel.consumeEach { closeableFrame ->
                    try {
                        audioStreamId?.let {
                            endpointInternal.write(
                                closeableFrame,
                                it
                            )
                        } ?: Logger.w(TAG, "Audio frame received but audio stream is not set")
                    } catch (t: Throwable) {
                        onInternalError(t)
                    }
                }
            }
        }
        if (withVideo) {
            coroutineScope.launch(videoOutputDispatcher) {
                // Video
                videoEncoderListener.outputChannel.consumeEach { closeableFrame ->
                    try {
                        videoStreamId?.let {
                            endpointInternal.write(
                                closeableFrame,
                                it
                            )
                        } ?: Logger.w(TAG, "Video frame received but video stream is not set")
                    } catch (t: Throwable) {
                        onInternalError(t)
                    }
                }
            }
        }
    }

    private val _audioCodecConfigFlow = MutableStateFlow<AudioCodecConfig?>(null)
    override val audioCodecConfigFlow = _audioCodecConfigFlow.asStateFlow()
    override val audioSourceConfigFlow = audioCodecConfigFlow.map { it?.sourceConfig }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        null
    )

    private val audioCodecConfig: AudioCodecConfig?
        get() = audioCodecConfigFlow.value

    override suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig) {
        require(withAudio) { "Audio is not enabled" }
        withContextMutex {
            setAudioCodecConfigUnsafe(audioCodecConfig)
        }
    }

    override suspend fun queueAudioFrame(frame: RawFrame) = mutex.withLock {
        val input = try {
            requireNotNull(audioInput) { "Audio input is null" }
        } catch (t: Throwable) {
            frame.close()
            throw t
        }
        input.queueInputFrame(
            frame
        )
    }

    private suspend fun setAudioCodecConfigUnsafe(audioCodecConfig: AudioCodecConfig) {
        require(!isStreaming) { "Can't change audio configuration while streaming" }

        if (this.audioCodecConfig == audioCodecConfig) {
            Logger.i(TAG, "Audio configuration is the same, skipping configuration")
            return
        }

        audioConfigEventListener?.onSetAudioSourceConfig(audioCodecConfig.sourceConfig)

        try {
            applyAudioCodecConfig(audioCodecConfig)
            _audioCodecConfigFlow.emit(audioCodecConfig)
        } catch (t: Throwable) {
            _audioCodecConfigFlow.emit(null)
            throw t
        }
    }

    private suspend fun applyAudioCodecConfig(audioConfig: AudioCodecConfig) {
        try {
            audioEncoderInternal?.release()
            audioEncoderInternal = buildAudioEncoder(audioConfig).apply {
                configure()
            }
        } catch (t: Throwable) {
            audioEncoderInternal?.release()
            audioEncoderInternal = null
            throw t
        }
    }

    private fun buildAudioEncoder(audioConfig: AudioCodecConfig): IEncoderInternal {
        val encoderMode = if (audioFrameRequestedListener != null) {
            EncoderMode.ASYNC
        } else {
            EncoderMode.SYNC
        }
        val audioEncoder = MediaCodecEncoder(
            AudioEncoderConfig(
                audioConfig, encoderMode
            ),
            audioEncoderListener,
            dispatcherProvider.default,
            audioEncoderDispatcher
        )

        when (audioEncoder.input) {
            is MediaCodecEncoder.AsyncByteBufferInput -> {
                audioEncoder.input.listener = requireNotNull(audioFrameRequestedListener) {
                    "Audio frame requested listener is not set"
                }
            }

            is MediaCodecEncoder.SyncByteBufferInput -> Unit

            else -> {
                throw UnsupportedOperationException("Unknown audio input type: ${audioEncoder.input}")
            }
        }

        return audioEncoder
    }

    private val _videoCodecConfigFlow = MutableStateFlow<VideoCodecConfig?>(null)
    override val videoCodecConfigFlow = _videoCodecConfigFlow.asStateFlow()

    override val videoSourceConfigFlow: StateFlow<VideoSourceConfig?> =
        videoCodecConfigFlow.map { it?.sourceConfig }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            null
        )

    private val videoCodecConfig: VideoCodecConfig?
        get() = videoCodecConfigFlow.value

    override suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig) {
        require(withVideo) { "Video is not enabled" }
        withContextMutex {
            setVideoCodecConfigUnsafe(videoCodecConfig)
        }
    }

    private suspend fun setVideoCodecConfigUnsafe(videoCodecConfig: VideoCodecConfig) {
        require(!isStreaming) { "Can't change video configuration while streaming" }

        if (this.videoCodecConfig == videoCodecConfig) {
            Logger.i(TAG, "Video configuration is the same, skipping configuration")
            return
        }

        videoConfigEventListener?.onSetVideoSourceConfig(videoCodecConfig.sourceConfig)

        try {
            applyVideoCodecConfig(videoCodecConfig)
            _videoCodecConfigFlow.emit(videoCodecConfig)
        } catch (t: Throwable) {
            _videoCodecConfigFlow.emit(null)
            throw t
        }
    }

    private suspend fun applyVideoCodecConfig(videoConfig: VideoCodecConfig) {
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

    private suspend fun buildAndConfigureVideoEncoder(
        videoConfig: VideoCodecConfig, @RotationValue targetRotation: Int
    ): IEncoderInternal {
        val rotatedVideoConfig =
            videoConfig.rotateFromNaturalOrientation(context, targetRotation)

        // Release codec instance
        videoEncoderInternal?.let { encoder ->
            if (encoder.input is MediaCodecEncoder.SurfaceInput) {
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
                videoConfig,
                EncoderMode.SURFACE
            ),
            videoEncoderListener,
            dispatcherProvider.default,
            videoEncoderDispatcher
        )

        when (videoEncoder.input) {
            is MediaCodecEncoder.SurfaceInput -> {
                videoEncoder.input.listener =
                    object : IEncoderInternal.ISurfaceInput.OnSurfaceUpdateListener {
                        override fun onSurfaceUpdated(surface: Surface) {
                            _surfaceFlow.tryEmit(
                                SurfaceDescriptor(
                                    surface,
                                    videoConfig.resolution,
                                    targetRotation,
                                    true
                                )
                            )
                        }
                    }
            }

            else -> {
                throw UnsupportedOperationException("Unknown video input type: ${videoEncoder.input}")
            }
        }

        return videoEncoder
    }

    /**
     * Opens the output endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    override suspend fun open(descriptor: MediaDescriptor) = withContextMutex {
        endpointInternal.open(descriptor)
    }

    /**
     * Closes the streamer endpoint.
     */
    override suspend fun close() = withContextMutex {
        stopStreamUnsafe()
        endpointInternal.close()
    }

    /**
     * Starts audio and/or video streams without a concurrent lock.
     *
     * @see [stopStream]
     */
    private suspend fun startStreamUnsafe() {
        if (isStreaming) {
            Logger.i(TAG, "Stream is already running")
            return
        }
        require(isOpenFlow.value) { "Endpoint must be opened before starting stream" }
        require(withAudio || withVideo) { "At least one of audio or video must be set" }
        if (withAudio) {
            requireNotNull(audioCodecConfig) { "Audio configuration must be set" }
        }
        if (withVideo) {
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

            streamEventListener?.onStartStream()

            val audioEncoderJob = audioEncoderInternal?.let {
                coroutineScope.launch {
                    it.startStream()
                }
            }

            val videoEncoderJob = videoEncoderInternal?.let {
                coroutineScope.launch {
                    it.startStream()
                }
            }

            audioEncoderJob?.join()
            videoEncoderJob?.join()

            endpointInternal.startStream()

            bitrateRegulatorController?.start()
        } catch (t: Throwable) {
            stopStreamUnsafe()
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
    override suspend fun startStream() {
        withContextMutex {
            startStreamUnsafe()
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
    override suspend fun stopStream() = withContextMutex {
        stopStreamUnsafe()
    }

    /**
     * Stops audio/video and reset stream implementation.
     *
     * @see [stopStream]
     */
    private suspend fun stopStreamUnsafe() {
        if (!isStreaming) {
            Logger.i(TAG, "Stream is already stopped")
            return
        }

        streamEventListener?.onStopStream()

        stopStreamElements()

        _isStreamingFlow.emit(false)
    }

    private suspend fun resetVideoEncoder() {
        _surfaceFlow.emit(null)

        pendingTargetRotation?.let {
            setTargetRotationInternal(it)
        } ?: videoEncoderInternal?.reset()
        pendingTargetRotation = null
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
        val audioEncoderJob = audioEncoderInternal?.let {
            coroutineScope.launch {
                try {
                    it.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "Can't stop audio encoder: ${t.message}")
                } finally {
                    audioStreamId = null
                }
                // Flush remaining frames
                audioEncoderListener.outputChannel.flush()

                try {
                    it.reset()
                } catch (t: Throwable) {
                    Logger.w(TAG, "Can't reset audio encoder: ${t.message}")
                }
            }
        }

        val videoEncoderJob = videoEncoderInternal?.let {
            coroutineScope.launch {
                try {
                    it.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "Can't stop video encoder: ${t.message}")
                } finally {
                    videoStreamId = null
                }

                // Flush remaining frames
                videoEncoderListener.outputChannel.flush()

                try {
                    resetVideoEncoder()
                } catch (t: Throwable) {
                    Logger.w(TAG, "Can't reset video encoder: ${t.message}")
                }
            }
        }

        audioEncoderJob?.join()
        videoEncoderJob?.join()

        // Endpoint
        try {
            endpointInternal.stopStream()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't stop endpoint: ${t.message}")
        }
    }

    private suspend fun releaseUnsafe() {
        _isStreamingFlow.emit(false)

        // Encoders
        try {
            audioEncoderInternal?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't release audio encoder: ${t.message}")
        } finally {
            audioEncoderInternal = null
            audioEncoderListener.outputChannel.cancel()
        }

        try {
            videoEncoderInternal?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't release video encoder: ${t.message}")
        } finally {
            _surfaceFlow.tryEmit(null)
            videoEncoderInternal = null
            videoEncoderListener.outputChannel.cancel()
        }

        // Endpoint
        try {
            endpointInternal.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "Can't release endpoint: ${t.message}")
        }

        audioOutputDispatcher.cancel()
        audioEncoderDispatcher.cancel()
        videoOutputDispatcher.cancel()
        videoEncoderDispatcher.cancel()

        coroutineScope.cancel()
    }

    /**
     * Releases endpoint and encoders.
     */
    override suspend fun release() = withContext(coroutineDispatcher) {
        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
        }
        mutex.withLock {
            releaseUnsafe()
        }
        coroutineScope.cancel()
    }

    /**
     * Adds a bitrate regulator controller.
     *
     * Limitation: it is only available for SRT for now.
     */
    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Output is released")
        }
        bitrateRegulatorController?.stop()
        bitrateRegulatorController =
            controllerFactory.newBitrateRegulatorController(this, dispatcherProvider.default)
                .apply {
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
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Output is released")
        }
        bitrateRegulatorController?.stop()
        bitrateRegulatorController = null
        Logger.d(TAG, "Bitrate regulator controller removed")
    }

    private suspend fun setTargetRotationInternal(@RotationValue newTargetRotation: Int) {
        if (shouldUpdateRotation(newTargetRotation)) {
            updateVideoEncoderForTransformation()
        }
    }

    private suspend fun updateVideoEncoderForTransformation() {
        val videoConfig = videoCodecConfig
        if (videoConfig != null) {
            applyVideoCodecConfig(videoConfig)
        }
    }

    /**
     * @return true if the target rotation has changed
     */
    private fun shouldUpdateRotation(@RotationValue newTargetRotation: Int): Boolean {
        return if (targetRotation != newTargetRotation) {
            targetRotation = newTargetRotation
            true
        } else {
            false
        }
    }

    /**
     * Executes a block with the [coroutineDispatcher] and the [mutex] locked.
     */
    private suspend fun withContextMutex(block: suspend () -> Unit) =
        withContext(coroutineDispatcher) {
            if (isReleaseRequested.get()) {
                throw IllegalStateException("Output is released")
            }
            mutex.withLock {
                block()
            }
        }

    override fun toString(): String {
        return "EncodingPipelineOutput(" +
                "withAudio=$withAudio, " +
                "withVideo=$withVideo, " +
                "isStreaming=$isStreaming, " +
                "audioCodecConfig=${audioCodecConfigFlow.value}, " +
                "videoCodecConfig=${videoCodecConfigFlow.value}, " +
                "targetRotation=$targetRotation, " +
                "isOpen=${isOpenFlow.value}, " +
                "bitrateRegulatorController=$bitrateRegulatorController" +
                ")"
    }

    companion object {
        private const val TAG = "EncodingPipelineOutput"
    }
}
