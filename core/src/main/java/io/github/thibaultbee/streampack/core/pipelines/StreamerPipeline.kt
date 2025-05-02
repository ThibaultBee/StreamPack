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
import android.graphics.Rect
import android.util.Size
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.data.ICloseableFrame
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AbstractSurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AspectRatioMode
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.SurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.source.DefaultSourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isCompatibleWith
import io.github.thibaultbee.streampack.core.elements.utils.extensions.runningHistoryNotNull
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoRotation
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline.AudioOutputMode
import io.github.thibaultbee.streampack.core.pipelines.inputs.AudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.VideoInput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioCallbackPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioSyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoCallbackPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.pipelines.utils.MultiException
import io.github.thibaultbee.streampack.core.pipelines.utils.SourceConfigUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main pipeline for the streamer.
 *
 * @param context the application context
 * @param withAudio whether the streamer has audio. It will create necessary audio components.
 * @param withVideo whether the streamer has video. It will create necessary video components.
 * @param audioOutputMode the audio output mode. It can be [AudioOutputMode.PUSH] or [AudioOutputMode.CALLBACK]. Only use [AudioOutputMode.CALLBACK] when you have a single output and its implements [IAudioCallbackPipelineOutputInternal]. By default, it is [AudioOutputMode.PUSH].
 * @param coroutineDispatcher the coroutine dispatcher
 */
open class StreamerPipeline(
    protected val context: Context,
    val withAudio: Boolean = true,
    val withVideo: Boolean = true,
    private val audioOutputMode: AudioOutputMode = AudioOutputMode.PUSH,
    protected val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IWithVideoSource, IWithVideoRotation, IWithAudioSource, IStreamer {
    private val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatcher)
    private var isReleaseRequested = AtomicBoolean(false)

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow = _throwableFlow.asStateFlow()

    // INPUTS
    private val audioInput = if (withAudio) {
        when (audioOutputMode) {
            AudioOutputMode.PUSH -> AudioInput(context, AudioInput.PushConfig(::queueAudioFrame))
            AudioOutputMode.CALLBACK -> AudioInput(context, AudioInput.CallbackConfig())
        }
    } else {
        null
    }

    /**
     * The audio source.
     * It allows access to advanced audio source settings.
     */
    override val audioSourceFlow: StateFlow<IAudioSource?> =
        audioInput?.audioSourceFlow ?: MutableStateFlow(null)

    /**
     * The audio processor.
     * It allows access to advanced audio settings.
     */
    override val audioProcessor: IAudioFrameProcessor? = audioInput?.audioProcessor

    private val videoInput = if (withVideo) {
        VideoInput(context)
    } else {
        null
    }

    /**
     * The video source.
     * It allows access to advanced video source settings.
     */
    override val videoSourceFlow: StateFlow<IVideoSource?> =
        videoInput?.videoSourceFlow ?: MutableStateFlow(null)


    private val _isStreamingFlow = MutableStateFlow(false)

    /**
     * State flow of the streaming state.
     *
     * It is true if a least one input (audio or video) is streaming.
     */
    override val isStreamingFlow: StateFlow<Boolean> = _isStreamingFlow.asStateFlow()

    // OUTPUTS
    private val outputMapMutex = Mutex()
    private val outputs = hashMapOf<IPipelineOutput, CoroutineScope>()

    /**
     * Sets the target rotation of all outputs.
     */
    override suspend fun setTargetRotation(
        @RotationValue rotation: Int
    ) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }

        require(withVideo) { "Do not need to set video rotation as it is an audio only streamer" }
        safeOutputCall { outputs ->
            outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                .forEach { it.setTargetRotation(rotation) }
        }
    }

    init {
        require(withAudio || withVideo) { "At least one of audio or video must be set" }

        videoInput?.let { input ->
            coroutineScope.launch {
                input.infoProviderFlow.collect {
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping video info")
                        return@collect
                    }
                    resetSurfaceProcessorOutputSurface()
                }
            }
            coroutineScope.launch {
                input.isStreamingFlow.collect { isStreaming ->
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping video streaming state")
                        return@collect
                    }

                    if (!isStreaming) {
                        if (audioInput?.isStreamingFlow?.value == true) {
                            Logger.i(TAG, "Stopping video only outputs")
                            // Only stops video only output
                            safeStreamingOutputCall { streamingOutputs ->
                                streamingOutputs.filter { !it.withAudio && it.isStreaming }
                                    .forEach { it.stopStream() }
                            }
                        } else {
                            Logger.i(TAG, "Stopping all outputs")
                            // Stops all outputs
                            _isStreamingFlow.emit(false)
                            stopOutputStreams()
                        }
                    }
                }
            }
        }

        audioInput?.let { input ->
            coroutineScope.launch {
                input.isStreamingFlow.collect { isStreaming ->
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping audio streaming state")
                        return@collect
                    }

                    if (!isStreaming) {
                        if (videoInput?.isStreamingFlow?.value == true) {
                            Logger.i(TAG, "Stopping audio only outputs")
                            // Stops audio only output
                            safeStreamingOutputCall { streamingOutputs ->
                                streamingOutputs.filter { !it.withVideo && it.isStreaming }
                                    .forEach { it.stopStream() }
                            }
                        } else {
                            Logger.i(TAG, "Stopping all outputs")
                            // Stops all outputs
                            _isStreamingFlow.emit(false)
                            stopOutputStreams()
                        }
                    }
                }
            }
        }
    }

    override suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withAudio) { "Do not need to set audio as it is a video only streamer" }
        val input = requireNotNull(audioInput) { "Audio input is not set" }
        input.setAudioSource(audioSourceFactory)
    }

    private suspend fun setAudioSourceConfig(value: AudioSourceConfig) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withAudio) { "Do not need to set audio as it is a video only streamer" }
        val input = requireNotNull(audioInput) { "Audio input is not set" }
        input.setAudioSourceConfig(value)
    }

    private fun queueAudioFrame(frame: RawFrame) {
        /**
         *  Using `runBlocking` to avoid to dispatch the frame to another thread.
         *  It is possible because the [RawFramePullPush] has an output thread.
         */
        runBlocking {
            safeStreamingOutputCall { streamingOutputs ->
                val audioStreamingOutput =
                    streamingOutputs.filterIsInstance<IAudioSyncPipelineOutputInternal>()
                if (audioStreamingOutput.isEmpty()) {
                    Logger.w(TAG, "No audio streaming output to process the frame")
                    frame.close()
                } else if (audioStreamingOutput.size == 1) {
                    audioStreamingOutput.first().queueAudioFrame(frame)
                } else {
                    // Hook to close frame when all outputs have processed it
                    var numOfClosed = 0
                    val onClosed = { frame: ICloseableFrame ->
                        numOfClosed++
                        if (numOfClosed == audioStreamingOutput.size) {
                            frame.close()
                        }
                    }
                    audioStreamingOutput.forEachIndexed { index, output ->
                        output.queueAudioFrame(
                            frame.copy(
                                rawBuffer = if (index == audioStreamingOutput.lastIndex) {
                                    frame.rawBuffer
                                } else {
                                    frame.rawBuffer.duplicate()
                                },
                                onClosed = onClosed
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets the video source.
     *
     * The previous video source will be released unless its preview is still running.
     */
    override suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withVideo) { "Do not need to set video as it is an audio only streamer" }
        val input = requireNotNull(videoInput) { "Video input is not set" }
        input.setVideoSource(videoSourceFactory)
    }

    private suspend fun setVideoSourceConfig(value: VideoSourceConfig) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withVideo) { "Do not need to set video as it is an audio only streamer" }
        val input = requireNotNull(videoInput) { "Video input is not set" }
        input.setVideoSourceConfig(value)
    }

    // VIDEO PROCESSING
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
    private suspend fun resetSurfaceProcessorOutputSurface(
        videoOutput: IVideoSurfacePipelineOutputInternal
    ) {
        Logger.i(TAG, "Updating transformation")
        videoOutput.surfaceFlow.value?.let { surfaceWithSize ->
            videoInput?.removeOutputSurface(surfaceWithSize.surface)
            videoInput?.let { input ->
                input.addOutputSurface(
                    buildSurfaceOutput(
                        surfaceWithSize.surface,
                        surfaceWithSize.resolution,
                        videoOutput.targetRotation,
                        videoOutput::isStreaming,
                        input.infoProviderFlow.value
                    )
                )
            }
        }
    }

    /**
     * Creates a surface output for the given surface.
     *
     * Use it for additional processing.
     *
     * @param surface the encoder surface
     * @param resolution the resolution of the surface
     * @param targetRotation the target rotation of the surface
     * @param infoProvider the source info provider for internal processing
     */
    private fun buildSurfaceOutput(
        surface: Surface,
        resolution: Size,
        @RotationValue targetRotation: Int,
        isStreaming: () -> Boolean,
        infoProvider: ISourceInfoProvider?
    ): AbstractSurfaceOutput {
        val cropRect = Rect(0, 0, resolution.width, resolution.height)
        return SurfaceOutput(
            surface, resolution, isStreaming, SurfaceOutput.TransformationInfo(
                getAspectRatioMode(),
                targetRotation,
                cropRect,
                isMirroringRequired(),
                infoProvider ?: DefaultSourceInfoProvider()
            )
        )
    }

    /**
     * Gets the aspect ratio mode to calculate the viewport rectangle.
     */
    protected open fun getAspectRatioMode(): AspectRatioMode {
        return AspectRatioMode.PRESERVE
    }

    /**
     * Whether the output surface needs to be mirrored.
     */
    protected open fun isMirroringRequired(): Boolean {
        return false
    }

    private suspend fun getNumOfAudioStreamingOutputSafe(output: IPipelineOutput?): Int {
        return safeOutputCall { outputs ->
            outputs.keys.minus(output).filterIsInstance<IAudioPipelineOutputInternal>()
                .count { it.isStreaming && it.withAudio }
        }
    }

    private suspend fun getNumOfVideoStreamingOutputSafe(output: IPipelineOutput?): Int {
        return safeOutputCall { outputs ->
            outputs.keys.minus(output).filterIsInstance<IVideoPipelineOutputInternal>()
                .count { it.isStreaming && it.withVideo }
        }
    }

    /**
     * Creates and adds an output to the pipeline.
     *
     * @param withAudio whether the output has audio. If the [StreamerPipeline] does not have audio, it will be ignored.
     * @param withVideo whether the output has video. If the [StreamerPipeline] does not have video, it will be ignored.
     * @param endpointFactory the endpoint factory to add the output to
     * @param targetRotation the target rotation of the output
     *
     * @return the [EncodingPipelineOutput] created
     */
    fun createEncodingOutput(
        withAudio: Boolean = this.withAudio,
        withVideo: Boolean = this.withVideo,
        endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
        @RotationValue targetRotation: Int = context.displayRotation
    ): IConfigurableAudioVideoEncodingPipelineOutput {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withAudio || withVideo) { "At least one of audio or video must be set" }
        val withAudioCorrected = if (this.withAudio) {
            withAudio
        } else {
            false
        }
        val withVideoCorrected = if (this.withVideo) {
            withVideo
        } else {
            false
        }

        val output =
            EncodingPipelineOutput(
                context,
                withAudioCorrected,
                withVideoCorrected,
                endpointFactory,
                targetRotation
            )
        return addOutput(output)
    }

    /**
     * Adds an output.
     *
     * The output must not be already streaming. Also, audio and video source needs to be null.
     *
     * @param output the output to add
     * @return the [output] added (same as input)
     */
    fun <T : IPipelineOutput> addOutput(output: T): T {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require((output is IVideoPipelineOutputInternal) || (output is IAudioPipelineOutputInternal)) {
            "Output must be an audio or video output"
        }
        if (outputs.contains(output)) {
            Logger.w(TAG, "Output $output already added")
            return output
        }
        require(!output.isStreaming) { "Output $output is already streaming" }

        val scope = CoroutineScope(Dispatchers.Default)
        outputs[output] = scope

        try {
            addOutputImpl(output, scope)
        } catch (t: Throwable) {
            runBlocking {
                removeOutput(output)
            }
            throw t
        }
        return output
    }

    private fun addOutputImpl(output: IPipelineOutput, scope: CoroutineScope) {
        if (output is IPipelineEventOutputInternal) {
            require(output.streamEventListener == null) { "Output $output already have a listener" }
            output.streamEventListener = object : IPipelineEventOutputInternal.Listener {
                override suspend fun onStartStream() = withContext(coroutineDispatcher) {
                    require(audioInput?.audioSourceFlow?.value != null || videoInput?.videoSourceFlow?.value != null) {
                        "At least one audio or video source must be provided"
                    }
                    /**
                     * Verify if the source configuration is still valid with the output configuration.
                     * Another output could have changed the source configuration in the meantime.
                     */
                    if (output.withAudio) {
                        require(withAudio) { "Do not need to set audio as it is a video only streamer" }
                        if (output is IConfigurableAudioPipelineOutput) {
                            val input = requireNotNull(audioInput) { "Audio input is not set" }
                            val inputSourceConfig =
                                requireNotNull(input.audioSourceConfigFlow.value) { "Input audio source config is not set" }
                            val outputSourceConfig =
                                requireNotNull(output.audioSourceConfigFlow.value) {
                                    "Output audio source config is not set"
                                }
                            require(outputSourceConfig.isCompatibleWith(inputSourceConfig)) { "Output audio source config is not compatible with input audio source config" }
                        }
                    }
                    if (output.withVideo) {
                        require(withVideo) { "Do not need to set video as it is an audio only streamer" }
                        if (output is IConfigurableVideoPipelineOutput) {
                            val input = requireNotNull(videoInput) { "Video input is not set" }
                            val inputSourceConfig =
                                requireNotNull(input.videoSourceConfigFlow.value) { "Input video source config is not set" }
                            val outputSourceConfig =
                                requireNotNull(output.videoSourceConfigFlow.value) {
                                    "Output video codec config is not set"
                                }
                            require(outputSourceConfig.isCompatibleWith(inputSourceConfig)) { "Output video source config is not compatible with input video source config" }
                        }
                    }

                    // Start stream if it is not already started
                    startInputStreams(output)
                }

                override suspend fun onStopStream() = withContext(coroutineDispatcher) {
                    stopInputStreams(output)
                }
            }
        } else {
            scope.launch {
                output.isStreamingFlow.collect { isStreaming ->
                    if (isStreaming) {
                        if (output !is IPipelineEventOutputInternal) {
                            startInputStreams(output)
                        }
                    } else {
                        stopInputStreams(output)
                    }
                }
            }
        }

        if (output is IAudioPipelineOutputInternal) {
            if (withAudio) {
                val audioInput = requireNotNull(audioInput) { "Audio input is not set" }
                if (audioOutputMode == AudioOutputMode.CALLBACK) {
                    require(output is IAudioCallbackPipelineOutputInternal) {
                        "Output $output must be an audio callback output"
                    }
                    addAudioAsyncOutputIfNeeded(
                        audioInput,
                        output
                    )
                }
                if (output is IConfigurableAudioPipelineOutputInternal) {
                    addEncodingAudioOutput(output)
                }
            } else {
                Logger.w(TAG, "Pipeline has no audio")
            }
        }

        if (output is IVideoPipelineOutputInternal) {
            if (withVideo) {
                addVideoOutputIfNeeded(output, scope)
                if (output is IConfigurableVideoPipelineOutputInternal) {
                    addEncodingVideoOutput(output)
                }
            } else {
                Logger.w(TAG, "Pipeline has no video")
            }
        }
    }

    private suspend fun buildAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig? = null): AudioSourceConfig {
        val audioSourceConfigs = safeStreamingOutputCall { streamingOutputs ->
            streamingOutputs.filterIsInstance<IAudioPipelineOutputInternal>().mapNotNull {
                (it as? IConfigurableAudioPipelineOutputInternal)?.audioSourceConfigFlow?.value
            }.toMutableSet()
        }
        newAudioSourceConfig?.let { audioSourceConfigs.add(it) }
        return SourceConfigUtils.buildAudioSourceConfig(audioSourceConfigs)
    }

    private fun addAudioAsyncOutputIfNeeded(
        audioInput: AudioInput,
        output: IAudioCallbackPipelineOutputInternal
    ) {
        require(outputs.keys.filterIsInstance<IAudioPipelineOutputInternal>().size == 1) {
            "Only one audio output is allowed for sync source"
        }

        output.audioFrameRequestedListener = audioInput.audioFrameRequestedListener
    }

    private fun addEncodingAudioOutput(
        output: IConfigurableAudioPipelineOutputInternal
    ) {
        require(output.audioSourceConfigFlow.value == null) { "Output $output already have an audio source config" }

        // Apply future audio source config
        require(output.audioConfigEventListener == null) { "Output $output already have an audio listener" }
        output.audioConfigEventListener =
            object : IConfigurableAudioPipelineOutputInternal.Listener {
                override suspend fun onSetAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig) {
                    setAudioSourceConfig(buildAudioSourceConfig(newAudioSourceConfig))
                }
            }
    }

    private fun addVideoSurfaceOutputIfNeeded(
        output: IVideoSurfacePipelineOutputInternal, scope: CoroutineScope
    ) {
        scope.launch {
            output.surfaceFlow.runningHistoryNotNull()
                .collect { (previousSurface, newSurface) ->
                    Logger.i(TAG, "Surface changed")
                    if (previousSurface?.surface == newSurface?.surface) {
                        return@collect
                    }

                    val input = requireNotNull(videoInput) { "Video input is not set" }

                    previousSurface?.let {
                        Logger.i(TAG, "Removing previous surface: $previousSurface")
                        input.removeOutputSurface(it.surface)
                    }
                    newSurface?.let {
                        Logger.i(TAG, "Adding new surface: $newSurface")
                        input.addOutputSurface(
                            buildSurfaceOutput(
                                it.surface,
                                it.resolution,
                                output.targetRotation,
                                output::isStreaming,
                                input.infoProviderFlow.value
                            )
                        )
                    }
                }
        }
    }

    private fun addVideoOutputIfNeeded(
        output: IVideoPipelineOutputInternal, scope: CoroutineScope
    ) {
        if (withVideo) {
            when {
                output is IVideoSurfacePipelineOutputInternal -> {
                    addVideoSurfaceOutputIfNeeded(output, scope)
                }

                else -> throw NotImplementedError("Output type $output is not supported")
            }
        } else {
            Logger.w(TAG, "Pipeline has no video")
        }
    }

    private suspend fun buildVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig? = null): VideoSourceConfig {
        val videoSourceConfigs =
            safeStreamingOutputCall { streamingOutputs ->
                streamingOutputs.filterIsInstance<IVideoPipelineOutputInternal>()
                    .filterIsInstance<IConfigurableVideoPipelineOutputInternal>()
                    .mapNotNull { it.videoSourceConfigFlow.value }.toMutableSet()
            }
        newVideoSourceConfig?.let { videoSourceConfigs.add(it) }
        return SourceConfigUtils.buildVideoSourceConfig(videoSourceConfigs)
    }

    private fun addEncodingVideoOutput(
        output: IConfigurableVideoPipelineOutputInternal
    ) {
        require(output.videoSourceConfigFlow.value == null) { "Output $output already have a video source config" }

        // Apply future video source config
        require(output.videoConfigEventListener == null) { "Output $output already have a video listener" }
        output.videoConfigEventListener =
            object : IConfigurableVideoPipelineOutputInternal.Listener {
                override suspend fun onSetVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig) {
                    setVideoSourceConfig(buildVideoSourceConfig(newVideoSourceConfig))
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
        if (output is IPipelineEventOutputInternal) {
            output.streamEventListener = null
        }
        if (output is IAudioCallbackPipelineOutputInternal) {
            output.audioFrameRequestedListener = null
        }
        if (output is IVideoSurfacePipelineOutputInternal) {
            output.surfaceFlow.value?.let {
                videoInput?.removeOutputSurface(it.surface)
            }
        }
        if (output is IVideoCallbackPipelineOutputInternal) {
            output.videoFrameRequestedListener = null
        }
    }

    /**
     * Removes an output.
     *
     * It will stop the stream.
     */
    suspend fun removeOutput(output: IPipelineOutput) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
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

    private suspend fun startInputStreams(output: IPipelineOutput) =
        withContext(coroutineDispatcher) {
            if (output.withAudio) {
                require(audioSourceFlow.value != null) { "At least one audio source must be provided" }
            }
            if (output.withVideo) {
                require(videoSourceFlow.value != null) { "At least one video source must be provided" }
            }
            val isAudioSourceStreaming = if (output.withAudio) {
                val input = requireNotNull(audioInput)
                val wasStreaming = input.isStreamingFlow.value
                input.startStream()
                wasStreaming
            } else {
                false
            }
            if (output.withVideo) {
                val input = requireNotNull(videoInput)
                try {
                    input.startStream()
                } catch (t: Throwable) {
                    // Restore audio source state
                    if (!isAudioSourceStreaming) {
                        audioInput?.stopStream()
                    }
                    throw t
                }
            }
            _isStreamingFlow.emit(true)
        }

    /**
     * Try to start all streams.
     *
     * If an [IEncodingPipelineOutput] is not opened, it won't start the stream and will throw an
     * exception. But the other outputs will be started.
     */
    override suspend fun startStream() = withContext(coroutineDispatcher) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        val exceptions = mutableListOf<Throwable>()
        safeOutputCall { outputs ->
            outputs.keys.forEach {
                try {
                    it.startStream()
                } catch (t: Throwable) {
                    exceptions += t
                    Logger.w(TAG, "startStream: Can't start output $it: ${t.message}")
                }
            }
        }
        if (exceptions.isNotEmpty()) {
            if (exceptions.size == 1) {
                throw exceptions.first()
            } else {
                throw MultiException(exceptions)
            }
        }
    }

    /**
     * Stops all inputs streams.
     */
    private suspend fun stopInputStreams() = withContext(coroutineDispatcher) {
        try {
            try {
                audioInput?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio input: ${t.message}")
            }
            try {
                videoInput?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop video input: ${t.message}")
            }
        } finally {
            _isStreamingFlow.emit(false)
        }
    }

    /**
     * Stops input streams that are no longer needed.
     */
    private suspend fun stopInputStreams(output: IPipelineOutput) =
        withContext(coroutineDispatcher) {
            // If sources are not streaming, do nothing
            var isAudioSourceStreaming = audioInput != null && audioInput.isStreamingFlow.value
            var isVideoSourceStreaming = videoInput != null && videoInput.isStreamingFlow.value
            if (!isAudioSourceStreaming && !isVideoSourceStreaming) {
                return@withContext
            }

            // Stop audio input if it is the last audio output
            if (output.withAudio && getNumOfAudioStreamingOutputSafe(output) == 0) {
                Logger.d(TAG, "No more audio output. Stopping audio input.")
                try {
                    audioInput?.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop audio input: ${t.message}")
                }
            }
            // Stop video input if it is the last video output
            if (output.withVideo && getNumOfVideoStreamingOutputSafe(output) == 0) {
                Logger.d(TAG, "No more video output. Stopping video input.")
                try {
                    videoInput?.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop video input: ${t.message}")
                }
            }

            // set isStreamingFlow to false if no more inputs are streaming
            isAudioSourceStreaming = audioInput != null && audioInput.isStreamingFlow.value
            isVideoSourceStreaming = videoInput != null && videoInput.isStreamingFlow.value
            if (!isAudioSourceStreaming && !isVideoSourceStreaming) {
                _isStreamingFlow.emit(false)
            }
        }

    private suspend fun stopOutputStreams() {
        /**
         *  [stopInputStreams] is called when all outputs are stopped.
         */
        safeStreamingOutputCall { outputs ->
            outputs.forEach {
                try {
                    it.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop output $it: ${t.message}")
                }
            }
        }
    }

    /**
     * Stops all streams.
     *
     * It stops audio and video sources and calls [IPipelineOutput.stopStream] on all outputs.
     */
    override suspend fun stopStream() = withContext(coroutineDispatcher) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }

        // Sources
        stopInputStreams()

        // Outputs
        stopOutputStreams()
    }

    private suspend fun releaseSources() {
        try {
            audioInput?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release audio input: ${t.message}")
        }
        try {
            videoInput?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release video input: ${t.message}")
        }
    }

    /**
     * Releases all resources.
     *
     * It releases the audio and video sources and the processors.
     * It also calls [IPipelineOutput.release] on all outputs.
     */
    override suspend fun release() = withContext(coroutineDispatcher) {
        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
        }

        // Sources
        try {
            releaseSources()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release sources: ${t.message}")
        }

        // Outputs
        safeOutputCall { outputs ->
            outputs.entries.forEach { (output, scope) ->
                try {
                    detachOutput(output)
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't detach output $output: ${t.message}")
                }
                try {
                    output.release()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't release output $output: ${t.message}")
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

    private suspend fun <T> safeStreamingOutputCall(block: suspend (List<IPipelineOutput>) -> T) =
        safeOutputCall { outputs ->
            val streamingOutputs = outputs.keys.filter { it.isStreamingFlow.value }
            block(streamingOutputs)
        }

    companion object {
        private const val TAG = "StreamerPipeline"
    }

    /**
     * Audio output mode.
     */
    enum class AudioOutputMode {
        /**
         * The audio is pushed to the output.
         */
        PUSH,

        /**
         * The audio is pulled by the output.
         */
        CALLBACK,
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