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
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AbstractSurfaceOutput
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
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.inputs.AudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.VideoInput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioAsyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IAudioSyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoAsyncPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IVideoSurfacePipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.pipelines.utils.SourceConfigUtils
import io.github.thibaultbee.streampack.core.streamers.interfaces.IAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
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

/**
 * Base class of all streamers.
 *
 * @param context the application context
 * @param hasAudio whether the streamer has audio. It will create necessary audio components.
 * @param hasVideo whether the streamer has video. It will create necessary video components.
 * @param coroutineDispatcher the coroutine dispatcher
 */
open class StreamerPipeline(
    protected val context: Context,
    val hasAudio: Boolean = true,
    val hasVideo: Boolean = true,
    protected val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IVideoStreamer, IAudioStreamer {
    private val coroutineScope: CoroutineScope = CoroutineScope(coroutineDispatcher)

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    val throwableFlow = _throwableFlow.asStateFlow()

    // INPUTS
    private val audioInput = if (hasAudio) {
        AudioInput(context, ::queueAudioFrame)
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

    private val videoInput = if (hasVideo) {
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
    val isStreamingFlow: StateFlow<Boolean> = _isStreamingFlow.asStateFlow()

    // OUTPUTS
    private val outputMapMutex = Mutex()
    private val outputs = hashMapOf<IPipelineOutput, CoroutineScope>()

    /**
     * Sets the target rotation of all outputs.
     */
    var targetRotation: Int = context.displayRotation
        set(@RotationValue value) {
            require(hasVideo) { "Do not need to set video rotation as it is an audio only streamer" }
            coroutineScope.launch {
                safeOutputCall { outputs ->
                    outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                        .forEach { it.targetRotation = value }
                }
            }
            field = value
        }

    init {
        videoInput?.let { input ->
            coroutineScope.launch {
                input.infoProviderFlow.collect {
                    resetSurfaceProcessorOutputSurface()
                }
            }
        }
    }

    override suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) {
        require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
        val input = requireNotNull(audioInput) { "Audio input is not set" }
        input.setAudioSource(audioSourceFactory)
    }

    private suspend fun setAudioSourceConfig(value: AudioSourceConfig) {
        require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
        val input = requireNotNull(audioInput) { "Audio input is not set" }
        input.setAudioSourceConfig(value)
    }

    private fun queueAudioFrame(frame: RawFrame) {
        val streamingOutputs = runBlocking {
            getStreamingOutputs()
        }
        streamingOutputs.filterIsInstance<IAudioSyncPipelineOutputInternal>().forEach {
            it.queueAudioFrame(frame.copy(buffer = frame.buffer.duplicate()))
        }
    }

    /**
     * Sets the video source.
     *
     * The previous video source will be released unless its preview is still running.
     */
    override suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory) {
        require(hasVideo) { "Do not need to set video as it is an audio only streamer" }
        val input = requireNotNull(videoInput) { "Video input is not set" }
        input.setVideoSource(videoSourceFactory)
    }

    private suspend fun setVideoSourceConfig(value: VideoSourceConfig) {
        require(hasVideo) { "Do not need to set video as it is an audio only streamer" }
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
        videoOutput.surfaceFlow.value?.let {
            videoInput?.removeOutputSurface(it.surface)
        }

        videoInput?.addOutputSurface(buildSurfaceOutput(videoOutput))
    }

    private fun buildSurfaceOutput(
        videoOutput: IVideoSurfacePipelineOutputInternal
    ): AbstractSurfaceOutput {
        val input = requireNotNull(videoInput) { "Video input is not set" }
        val surfaceWithSize = requireNotNull(videoOutput.surfaceFlow.value) {
            "Output $videoOutput has no surface"
        }

        return buildSurfaceOutput(
            surfaceWithSize.surface,
            surfaceWithSize.resolution,
            videoOutput::isStreaming,
            input.infoProviderFlow.value
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
        infoProvider: ISourceInfoProvider?
    ): AbstractSurfaceOutput {
        return SurfaceOutput(
            surface, resolution, isStreaming, SurfaceOutput.TransformationInfo(
                targetRotation, isMirroringRequired(), infoProvider ?: DefaultSourceInfoProvider()
            )
        )
    }

    /**
     * Whether the output surface needs to be mirrored.
     */
    protected open fun isMirroringRequired(): Boolean {
        return false
    }

    private fun getNumOfAudioStreamingOutput(output: IPipelineOutput?) =
        outputs.keys.minus(output).filterIsInstance<IAudioPipelineOutputInternal>()
            .count { it.isStreaming && it.hasAudio }


    private suspend fun getNumOfAudioStreamingOutputSafe(output: IPipelineOutput?): Int {
        return safeOutputCall {
            getNumOfAudioStreamingOutput(output)
        }
    }

    private fun getNumOfVideoStreamingOutput(output: IPipelineOutput?) =
        outputs.keys.minus(output).filterIsInstance<IVideoPipelineOutputInternal>()
            .count { it.isStreaming && it.hasVideo }

    private suspend fun getNumOfVideoStreamingOutputSafe(output: IPipelineOutput?): Int {
        return safeOutputCall {
            getNumOfVideoStreamingOutput(output)
        }
    }


    /**
     * Creates and adds an output to the pipeline.
     *
     * @param endpointFactory the endpoint factory to add the output to
     * @param targetRotation the target rotation of the output
     *
     * @return the [EncodingPipelineOutput] created
     */
    fun addOutput(
        endpointFactory: IEndpointInternal.Factory,
        @RotationValue targetRotation: Int = context.displayRotation
    ): IEncodingPipelineOutput {
        val output =
            EncodingPipelineOutput(context, hasAudio, hasVideo, endpointFactory, targetRotation)
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
                    if (output.hasAudio) {
                        require(hasAudio) { "Do not need to set audio as it is a video only streamer" }
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
                    if (output.hasVideo) {
                        require(hasVideo) { "Do not need to set video as it is an audio only streamer" }
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
            if (hasAudio) {
                if ((output !is IAudioSyncPipelineOutputInternal) && (output is IAudioAsyncPipelineOutputInternal)) {
                    addAudioAsyncOutputIfNeeded(output)
                }
                if (output is IConfigurableAudioPipelineOutputInternal) {
                    addEncodingAudioOutput(output)
                }
            } else {
                Logger.w(TAG, "Pipeline has no audio")
            }
        }

        if (output is IVideoPipelineOutputInternal) {
            if (hasVideo) {
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
        val audioSourceConfigs =
            getStreamingOutputs().filterIsInstance<IAudioPipelineOutputInternal>().mapNotNull {
                (it as? IConfigurableAudioPipelineOutputInternal)?.audioSourceConfigFlow?.value
            }.toMutableSet()
        newAudioSourceConfig?.let { audioSourceConfigs.add(it) }
        return SourceConfigUtils.buildAudioSourceConfig(audioSourceConfigs)
    }

    private fun addAudioAsyncOutputIfNeeded(output: IAudioAsyncPipelineOutputInternal) {
        require(outputs.keys.filterIsInstance<IAudioPipelineOutputInternal>().size == 1) {
            "Only one output is allowed for frame source"
        }

        throw NotImplementedError("Audio async output not supported yet")
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
            output.surfaceFlow.runningHistoryNotNull().collect { (previousSurface, newSurface) ->
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
        if (hasVideo) {
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
            getStreamingOutputs().filterIsInstance<IVideoPipelineOutputInternal>()
                .filterIsInstance<IConfigurableVideoPipelineOutputInternal>()
                .mapNotNull { it.videoSourceConfigFlow.value }.toMutableSet()
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
        if (output is IAudioAsyncPipelineOutputInternal) {
            output.audioFrameRequestedListener = null
        }
        if (output is IVideoSurfacePipelineOutputInternal) {
            output.surfaceFlow.value?.let {
                videoInput?.removeOutputSurface(it.surface)
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

    private suspend fun startInputStreams(output: IPipelineOutput) =
        withContext(coroutineDispatcher) {
            if (output.hasAudio) {
                require(audioSourceFlow.value != null) { "At least one audio source must be provided" }
            }
            if (output.hasVideo) {
                require(videoSourceFlow.value != null) { "At least one video source must be provided" }
            }
            val isAudioSourceStreaming = if (output.hasAudio) {
                val input = requireNotNull(audioInput)
                val wasStreaming = input.isStreamingFlow.value
                input.startStream()
                wasStreaming
            } else {
                false
            }
            if (output.hasVideo) {
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
            if (output.hasAudio && getNumOfAudioStreamingOutputSafe(output) == 0) {
                Logger.d(TAG, "No more audio output. Stopping audio input.")
                try {
                    audioInput?.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop audio input: ${t.message}")
                }
            }
            // Stop video input if it is the last video output
            if (output.hasVideo && getNumOfVideoStreamingOutputSafe(output) == 0) {
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
        getStreamingOutputs().forEach {
            try {
                it.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop output $it: ${t.message}")
            }
        }
    }

    /**
     * Stops all streams.
     *
     * It stops audio and video sources and calls [IPipelineOutput.stopStream] on all outputs.
     */
    suspend fun stopStream() = withContext(coroutineDispatcher) {
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
        private const val TAG = "StreamerPipeline"
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