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
package io.github.thibaultbee.streampack.core.pipelines

import android.content.Context
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoderInternal
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.audio.AudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.SurfaceProcessor
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AbstractSurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.SurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isCompatibleWith
import io.github.thibaultbee.streampack.core.elements.utils.extensions.runningHistory
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioAsyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioSyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoAsyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.pipelines.utils.SourceConfigUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Base class of all streamers.
 *
 * @param context the application context
 * @param videoSourceInternal the video source implementation
 * @param audioSourceInternal the audio source implementation
 */
open class StreamerPipeline(
    protected val context: Context,
    protected val audioSourceInternal: IAudioSourceInternal?,
    protected val videoSourceInternal: IVideoSourceInternal?,
    protected val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    protected val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatcher)

    private var surfaceProcessor: ISurfaceProcessorInternal? = null
    private val audioProcessorInternal = AudioFrameProcessor(::queueAudioFrame)

    val audioProcessor: IAudioFrameProcessor
        get() = audioProcessorInternal

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    val throwableFlow = _throwableFlow.asStateFlow()

    private val sourceInfoProvider: ISourceInfoProvider?
        get() = videoSourceInternal?.infoProviderFlow?.value

    // SOURCES
    private val audioSourceMutex = Mutex()
    private val videoSourceMutex = Mutex()

    /**
     * The audio source.
     * It allows advanced audio settings.
     */
    val audioSource: IAudioSource?
        get() = audioSourceInternal

    /**
     * The video source.
     * It allows advanced video settings.
     */
    val videoSource: IVideoSource?
        get() = videoSourceInternal

    /**
     * Flow of the streaming state.
     */
    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val numOfStreamingOutput: Int
        get() = outputs.keys.count { it.isStreamingFlow.value }

    /**
     * Whether the streamer has audio.
     */
    val hasAudio = audioSourceInternal != null

    /**
     * Whether the streamer has video.
     */
    val hasVideo = videoSourceInternal != null

    // OUTPUTS
    private val outputMapMutex = Mutex()
    private val outputs = hashMapOf<IPipelineOutput, CoroutineScope>()

    /**
     * Sets the target rotation of all outputs.s
     */
    var targetRotation: Int = context.displayRotation
        set(@RotationValue value) {
            coroutineScope.launch {
                safeOutputCall { outputs ->
                    outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                        .forEach { it.targetRotation = value }
                }
            }
            field = value
        }


    init {
        setSources()
    }

    private fun queueAudioFrame(frame: Frame) {
        val streamingOutputs = runBlocking {
            getStreamingOutputs()
        }
        streamingOutputs.filterIsInstance<IAudioSyncPipelineOutputInternal>()
            .forEach {
                it.queueAudioFrame(frame.copy(rawBuffer = frame.rawBuffer.duplicate()))
            }
    }

    private var _audioSourceConfig: AudioSourceConfig? = null
    private val audioSourceConfig: AudioSourceConfig
        get() = requireNotNull(_audioSourceConfig) { "Audio source config is not set" }

    private suspend fun setAudioSourceConfig(value: AudioSourceConfig) =
        withContext(coroutineDispatcher) {
            audioSourceMutex.withLock {
                require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
                if (_audioSourceConfig == value) {
                    Logger.i(TAG, "Audio source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change audio source configuration while streaming" }

                _audioSourceConfig = value
                applyAudioSourceConfig(value)
            }
        }

    private fun applyAudioSourceConfig(audioConfig: AudioSourceConfig) {
        try {
            audioSourceInternal?.configure(audioConfig)
        } catch (t: Throwable) {
            throw t
        }
    }

    private var _videoSourceConfig: VideoSourceConfig? = null
    private val videoSourceConfig: VideoSourceConfig
        get() = requireNotNull(_videoSourceConfig) { "Video source config is not set" }

    private suspend fun setVideoSourceConfig(value: VideoSourceConfig) =
        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                require(hasVideo) { "Do not need to set video as it is a audio only streamer" }
                if (_videoSourceConfig == value) {
                    Logger.i(TAG, "Video source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change video source configuration while streaming" }

                val previousVideoConfig = _videoSourceConfig
                _videoSourceConfig = value
                applyVideoSourceConfig(previousVideoConfig, value)
            }
        }

    private suspend fun applyVideoSourceConfig(
        previousVideoConfig: VideoSourceConfig?, videoConfig: VideoSourceConfig
    ) {
        try {
            videoSourceInternal?.configure(videoConfig)

            // Update surface processor
            if (videoSourceInternal is ISurfaceSource) {
                val currentSurfaceProcessor = surfaceProcessor
                if (currentSurfaceProcessor == null) {
                    surfaceProcessor = buildSurfaceProcessor(videoConfig)
                } else if (previousVideoConfig?.dynamicRangeProfile != videoConfig.dynamicRangeProfile) {
                    releaseSurfaceProcessor()
                    surfaceProcessor = buildSurfaceProcessor(videoConfig)
                } else if (previousVideoConfig.resolution != videoConfig.resolution) {
                    val outputSurface = requireNotNull(videoSourceInternal.outputSurface) {
                        "Video source must have an output surface"
                    }
                    currentSurfaceProcessor.updateInputSurface(
                        outputSurface, videoSourceInternal.infoProviderFlow.value.getSurfaceSize(
                            videoSourceConfig.resolution
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            throw t
        }
    }

    private fun buildSurfaceOutput(
        videoOutput: IVideoSurfacePipelineOutputInternal
    ): AbstractSurfaceOutput {
        val surfaceWithSize = requireNotNull(videoOutput.surfaceFlow.value) {
            "Output $videoOutput has no surface"
        }

        return buildSurfaceOutput(
            surfaceWithSize.surface,
            surfaceWithSize.resolution,
            videoOutput::isStreaming,
            sourceInfoProvider!!
        )
    }

    /**
     * Creates a surface output for the given surface.
     *
     * Use it for additional processing.
     *
     * @param surface the encoder surface
     * @param resolution the resolution of the surface
     * @param infoProvider the source info provider for internal processing
     */
    private fun buildSurfaceOutput(
        surface: Surface,
        resolution: Size,
        isStreaming: () -> Boolean,
        infoProvider: ISourceInfoProvider
    ): AbstractSurfaceOutput {
        return SurfaceOutput(
            surface, resolution, isStreaming, SurfaceOutput.TransformationInfo(
                targetRotation, isMirroringRequired(), infoProvider
            )
        )
    }

    /**
     * Whether the output surface needs to be mirrored.
     */
    protected open fun isMirroringRequired(): Boolean {
        return false
    }

    /**
     * Updates the transformation of the surface output.
     * To be called when the source info provider or [isMirroringRequired] is updated.
     */
    private suspend fun resetSurfaceProcessorOutputSurface() {
        safeOutputCall { outputs ->
            outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                .filter { it.surfaceFlow.value != null }.forEach {
                    resetSurfaceProcessorOutputSurface(it)
                }
        }
    }

    /**
     * Updates the transformation of the surface output.
     */
    private fun resetSurfaceProcessorOutputSurface(
        videoOutput: IVideoSurfacePipelineOutputInternal
    ) {
        Logger.i(TAG, "Updating transformation")
        videoOutput.surfaceFlow.value?.let {
            surfaceProcessor?.removeOutputSurface(it.surface)
        }

        surfaceProcessor?.addOutputSurface(buildSurfaceOutput(videoOutput))
    }

    private fun releaseSurfaceProcessor() {
        val videoSource = videoSourceInternal
        if (videoSource is ISurfaceSource) {
            videoSource.outputSurface?.let {
                surfaceProcessor?.removeInputSurface(it)
            }
        }
        surfaceProcessor?.removeAllOutputSurfaces()
        surfaceProcessor?.release()
    }

    private suspend fun buildSurfaceProcessor(
        videoSourceConfig: VideoSourceConfig
    ): ISurfaceProcessorInternal {
        val videoSource = videoSourceInternal
        if (videoSource !is ISurfaceSource) {
            throw IllegalStateException("Video source must have an output surface")
        }

        val newSurfaceProcessor = SurfaceProcessor(videoSourceConfig.dynamicRangeProfile)

        // Adds surface processor input
        videoSource.outputSurface = newSurfaceProcessor.createInputSurface(
            videoSource.infoProviderFlow.value.getSurfaceSize(
                videoSourceConfig.resolution
            )
        )

        // Adds surface processor output
        safeOutputCall { outputs ->
            outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                .filter { it.surfaceFlow.value != null }.forEach {
                    newSurfaceProcessor.addOutputSurface(buildSurfaceOutput(it))
                }
        }


        return newSurfaceProcessor
    }

    private fun setSources() {
        videoSourceInternal?.isStreamingFlow?.let {
            coroutineScope.launch {
                it.collect { isStreaming ->
                    if ((!isStreaming) && this@StreamerPipeline.isStreamingFlow.value) {
                        Logger.i(TAG, "Video source has been stopped. Stopping everything.")
                        stopStream()
                    }
                }
            }
        }
        videoSourceInternal?.infoProviderFlow?.let {
            coroutineScope.launch {
                it.collect {
                    resetSurfaceProcessorOutputSurface()
                }
            }
        }
        audioSourceInternal?.let { audioProcessorInternal.setInput(it::getAudioFrame) }
    }

    /**
     * Creates and adds an output to the pipeline.
     *
     * @param endpointFactory the endpoint factory to add the output to
     * @param targetRotation the target rotation of the output
     *
     * @return the [EncodingPipelineOutput] created
     */
    suspend fun addOutput(
        endpointFactory: IEndpointInternal.Factory,
        @RotationValue targetRotation: Int = context.displayRotation
    ): IEncodingPipelineOutput {
        val output = EncodingPipelineOutput(context, endpointFactory, targetRotation)
        return addOutput(output)
    }

    /**
     * Adds an output.
     *
     * @param output the output to add
     * @return the [output] added (same as input)
     */
    internal suspend fun <T : IPipelineOutput> addOutput(output: T): T {
        require((output is IVideoPipelineOutputInternal) || (output is IAudioPipelineOutputInternal)) {
            "Output must be an audio or video output"
        }
        if (outputs.contains(output)) {
            Logger.w(TAG, "Output $output already added")
            return output
        }

        val scope = CoroutineScope(Dispatchers.Default)
        safeOutputCall { outputs ->
            outputs[output] = scope
        }

        try {
            addOutputImpl(output, scope)
        } catch (t: Throwable) {
            safeOutputCall { outputs ->
                outputs.remove(output)
            }
            scope.cancel()
            throw t
        }
        return output
    }

    private suspend fun addOutputImpl(output: IPipelineOutput, scope: CoroutineScope) {
        if (output.isStreaming) {
            // Start stream if it is not already started
            if (!this@StreamerPipeline.isStreamingFlow.value) {
                startSourceStream()
            }
        }
        if (output is IPipelineOutputInternal) {
            require(output.streamEventListener == null) { "Output $output already have a listener" }
            output.streamEventListener = object : IPipelineOutputInternal.Listener {
                override suspend fun onStartStream() = withContext(coroutineDispatcher) {
                    /**
                     * Verify if the source configuration is still valid with the output configuration.
                     * Another output could have changed the source configuration in the meantime.
                     */
                    /**
                     * Verify if the source configuration is still valid with the output configuration.
                     * Another output could have changed the source configuration in the meantime.
                     */
                    if (output.hasAudio) {
                        if (output is IConfigurableAudioPipelineOutputInternal) {
                            requireNotNull(_audioSourceConfig) { "Audio source config is not set" }
                            require(
                                output.audioCodecConfigFlow.value!!.isCompatibleWith(
                                    audioSourceConfig
                                )
                            ) { "Audio codec config is not compatible with audio source config" }
                        }
                    }
                    if (output.hasVideo) {
                        if (output is IConfigurableVideoPipelineOutputInternal) {
                            requireNotNull(_videoSourceConfig) { "Video source config is not set" }
                            require(
                                output.videoCodecConfigFlow.value!!.isCompatibleWith(
                                    videoSourceConfig
                                )
                            ) { "Video codec config is not compatible with video source config" }
                        }
                    }

                    // Start stream if it is not already started
                    if (!this@StreamerPipeline.isStreamingFlow.value) {
                        startSourceStream()
                    }
                }

                override suspend fun onStopStream() = withContext(coroutineDispatcher) {
                    // -1 because the output is still streaming
                    if ((this@StreamerPipeline.isStreamingFlow.value) && ((numOfStreamingOutput - 1) == 0)) {
                        Logger.i(TAG, "All outputs have been stopped. Stopping sources.")
                        stopSourceStreams()
                    }
                }
            }
        } else {
            scope.launch {
                output.isStreamingFlow.collect { isStreaming ->
                    if (isStreaming) {
                        if ((!this@StreamerPipeline.isStreamingFlow.value) && (output !is IPipelineOutputInternal)) {
                            startSourceStream()
                        }
                    } else {
                        if ((this@StreamerPipeline.isStreamingFlow.value) && (numOfStreamingOutput == 0)) {
                            Logger.i(TAG, "All outputs have been stopped. Stopping sources.")
                            stopSourceStreams()
                        }
                    }
                }
            }
        }

        if (output is IAudioPipelineOutputInternal) {
            if ((output !is IAudioSyncPipelineOutputInternal) && (output is IAudioAsyncPipelineOutputInternal)) {
                addAudioAsyncOutputIfNeeded(output)
            }
            if (output is IConfigurableAudioPipelineOutputInternal) {
                addEncodingAudioOutput(output)
            }
        }

        if (output is IVideoPipelineOutputInternal) {
            addVideoOutputIfNeeded(output, scope)
            if (output is IConfigurableVideoPipelineOutputInternal) {
                addEncodingVideoOutput(output)
            }
        }
    }

    private suspend fun buildAudioSourceConfig(newAudioCodecConfig: AudioCodecConfig? = null): AudioSourceConfig {
        val audioCodecConfigs = getStreamingOutputs()
            .filterIsInstance<IAudioPipelineOutputInternal>()
            .mapNotNull {
                (it as? IConfigurableAudioPipelineOutputInternal)?.audioCodecConfigFlow?.value
            }.toMutableSet()
        newAudioCodecConfig?.let { audioCodecConfigs.add(it) }
        return SourceConfigUtils.buildAudioSourceConfig(audioCodecConfigs)
    }

    private fun addAudioAsyncOutputIfNeeded(output: IAudioAsyncPipelineOutputInternal) {
        require(outputs.keys.filterIsInstance<IAudioPipelineOutputInternal>().size == 1) {
            "Only one output is allowed for frame source"
        }

        Logger.w(TAG, "Audio processor is not supported for async pipeline")

        if (hasAudio) {
            output.audioFrameRequestedListener =
                object : IEncoderInternal.IAsyncByteBufferInput.OnFrameRequestedListener {
                    override fun onFrameRequested(buffer: ByteBuffer): Frame {
                        return audioSourceInternal!!.getAudioFrame(buffer)
                    }
                }
        } else {
            output.audioFrameRequestedListener = null
            Logger.w(TAG, "Output $output has audio but pipeline has no audio")
        }
    }

    private suspend fun addEncodingAudioOutput(
        output: IConfigurableAudioPipelineOutputInternal
    ) {
        // Apply already set audio source config
        output.audioCodecConfigFlow.value?.let { _ ->
            setAudioSourceConfig(buildAudioSourceConfig())
        }

        // Apply future audio source config
        require(output.audioConfigEventListener == null) { "Output $output already have an audio listener" }
        output.audioConfigEventListener =
            object : IConfigurableAudioPipelineOutputInternal.Listener {
                override suspend fun onSetAudioCodecConfig(newAudioCodecConfig: AudioCodecConfig) {
                    setAudioSourceConfig(buildAudioSourceConfig(newAudioCodecConfig))
                }
            }
    }

    private fun addVideoSurfaceOutputIfNeeded(
        output: IVideoSurfacePipelineOutputInternal, scope: CoroutineScope
    ) {
        when (videoSourceInternal) {
            is ISurfaceSource -> {
                output.videoSourceTimestampOffset = videoSourceInternal.timestampOffset
                scope.launch {
                    output.surfaceFlow.runningHistory()
                        .collect { (previousSurface, newSurface) ->
                            Logger.i(TAG, "Surface changed")
                            if (previousSurface?.surface == newSurface?.surface) {
                                return@collect
                            }

                            previousSurface?.let {
                                Logger.i(TAG, "Removing previous surface: $previousSurface")
                                surfaceProcessor?.removeOutputSurface(it.surface)
                            }
                            newSurface?.let {
                                Logger.i(TAG, "Adding new surface: $newSurface")
                                surfaceProcessor?.addOutputSurface(
                                    buildSurfaceOutput(
                                        it.surface,
                                        it.resolution,
                                        output::isStreaming,
                                        sourceInfoProvider!!
                                    )
                                )
                            }
                        }
                }
            }

            else -> throw IllegalStateException("Video source must be a surface or frame source")
        }
    }

    private fun addVideoOutputIfNeeded(
        output: IVideoPipelineOutputInternal, scope: CoroutineScope
    ) {
        if (hasVideo) {
            when {
                output is IVideoSurfacePipelineOutputInternal -> {
                    addVideoSurfaceOutputIfNeeded(output, scope)
                }

                else -> throw NotImplementedError("Output type $output is not supported")
            }
        } else {
            Logger.w(TAG, "Output $output has video but pipeline has no video")
        }
    }

    private suspend fun buildVideoSourceConfig(newVideoCodecConfig: VideoCodecConfig? = null): VideoSourceConfig {
        val videoCodecConfigs = getStreamingOutputs()
            .filterIsInstance<IVideoPipelineOutputInternal>()
            .filterIsInstance<IConfigurableVideoPipelineOutputInternal>()
            .mapNotNull { it.videoCodecConfigFlow.value }
            .toMutableSet()
        newVideoCodecConfig?.let { videoCodecConfigs.add(it) }
        return SourceConfigUtils.buildVideoSourceConfig(videoCodecConfigs)
    }

    private suspend fun addEncodingVideoOutput(
        output: IConfigurableVideoPipelineOutputInternal
    ) {
        // Apply already set video source config
        output.videoCodecConfigFlow.value?.let { _ ->
            setVideoSourceConfig(buildVideoSourceConfig())
        }

        // Apply future video source config
        require(output.videoConfigEventListener == null) { "Output $output already have a video listener" }
        output.videoConfigEventListener =
            object : IConfigurableVideoPipelineOutputInternal.Listener {
                override suspend fun onSetVideoCodecConfig(newVideoCodecConfig: VideoCodecConfig) {
                    setVideoSourceConfig(buildVideoSourceConfig(newVideoCodecConfig))
                }
            }
    }

    /**
     * Removes an output.
     *
     * It will stop the stream.
     */
    private suspend fun detachOutput(output: IPipelineOutput) {
        Logger.i(TAG, "Removing output $output")
        output.stopStream()

        // Clean streamer output
        if (output is IConfigurableVideoPipelineOutputInternal) {
            output.videoConfigEventListener = null
        }
        if (output is IConfigurableAudioPipelineOutputInternal) {
            output.audioConfigEventListener = null
        }
        if (output is IPipelineOutputInternal) {
            output.streamEventListener = null
        }
        if (output is IAudioAsyncPipelineOutputInternal) {
            output.audioFrameRequestedListener = null
        }
        if (output is IVideoSurfacePipelineOutputInternal) {
            output.surfaceFlow.value?.let {
                surfaceProcessor?.removeOutputSurface(it.surface)
            }
        }
        if (output is IVideoAsyncPipelineOutputInternal) {
            output.videoFrameRequestedListener = null
        }
    }

    /**
     * Removes an output.
     *
     * It will stop the stream.
     */
    suspend fun removeOutput(output: IPipelineOutput) {
        if (!outputs.contains(output)) {
            Logger.w(TAG, "Output $output not found")
            return
        }

        detachOutput(output)

        safeOutputCall { outputs ->
            outputs[output]?.cancel()
            outputs.remove(output)
        }
    }

    /**
     * Starts audio/video source.
     *
     * @see [stopStream]
     */
    private suspend fun startSourceStream() = withContext(coroutineDispatcher) {
        audioSourceMutex.lock()
        videoSourceMutex.lock()

        try {
            // Sources
            audioSourceInternal?.let {
                it.startStream()
                audioProcessorInternal.startStream()
            }

            videoSourceInternal?.startStream()

            _isStreamingFlow.emit(true)
        } catch (t: Throwable) {
            stopStream()
            throw t
        } finally {
            audioSourceMutex.unlock()
            videoSourceMutex.unlock()
        }
    }

    /**
     * Try to start all streams.
     *
     * If an [IEncodingPipelineOutput] is not opened, it won't start the stream.
     */
    suspend fun startStream() = withContext(coroutineDispatcher) {
        val streamingOutput = safeOutputCall {
            outputs.keys
        }
        streamingOutput.forEach {
            try {
                it.startStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "startStream: Can't start output $it: ${t.message}")
            }
        }
    }

    private suspend fun stopOutputStreams() {
        /**
         *  [stopSourceStreams] is called when all outputs are stopped.
         */
        getStreamingOutputs().forEach {
            try {
                it.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop output $it: ${t.message}")
            }
        }
    }

    private suspend fun stopSourceStreams() = withContext(coroutineDispatcher) {
        audioSourceMutex.lock()
        videoSourceMutex.lock()

        try {
            // Sources
            try {
                audioProcessorInternal.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio processor: ${t.message}")
            }
            try {
                audioSourceInternal?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio source: ${t.message}")
            }
            try {
                videoSourceInternal?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop video source: ${t.message}")
            }
        } finally {
            audioSourceMutex.unlock()
            videoSourceMutex.unlock()
            _isStreamingFlow.emit(false)
        }
    }

    /**
     * Stops all streams.
     *
     * It stops audio and video sources and calls [IPipelineOutput.stopStream] on all outputs.
     */
    suspend fun stopStream() = withContext(coroutineDispatcher) {
        // Sources
        stopSourceStreams()

        // Outputs
        stopOutputStreams()
    }

    private suspend fun releaseSources() {
        audioSourceMutex.withLock {
            audioProcessorInternal.removeInput()
            audioProcessorInternal.release()
            audioSourceInternal?.release()
        }
        videoSourceMutex.withLock {
            releaseSurfaceProcessor()
            val videoSource = videoSourceInternal
            if (videoSource is ISurfaceSource) {
                videoSource.outputSurface = null
            }
            videoSourceInternal?.release()
        }
    }

    /**
     * Releases all resources.
     *
     * It releases the audio and video sources and the processors.
     * It also calls [IPipelineOutput.release] on all outputs.
     */
    suspend fun release() = withContext(coroutineDispatcher) {
        // Sources
        try {
            releaseSources()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release sources: ${t.message}")
        }

        // Outputs
        safeOutputCall {
            outputs.entries.forEach { (output, scope) ->
                try {
                    detachOutput(output)
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't detach output $it: ${t.message}")
                }
                try {
                    output.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't release output $it: ${t.message}")
                }
                scope.cancel()
            }
            outputs.clear()
        }
        coroutineScope.cancel()
    }

    private suspend fun <T> safeOutputCall(block: suspend (MutableMap<IPipelineOutput, CoroutineScope>) -> T) =
        withContext(coroutineDispatcher) {
            outputMapMutex.withLock {
                block(outputs)
            }
        }

    private suspend fun getStreamingOutputs() = safeOutputCall { outputs ->
        outputs.keys.filter { it.isStreamingFlow.value }
    }

    companion object {
        const val TAG = "StreamerPipeline"
    }
}

/**
 * Clean and reset the pipeline synchronously.
 *
 * @see [StreamerPipeline.release]
 */
fun StreamerPipeline.releaseBlocking() = runBlocking {
    release()
}