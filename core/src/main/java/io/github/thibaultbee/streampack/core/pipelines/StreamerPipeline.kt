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
import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.DefaultSurfaceProcessorFactory
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
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
import io.github.thibaultbee.streampack.core.pipelines.inputs.AudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.IAudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.IVideoInput
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
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.isStreaming
import io.github.thibaultbee.streampack.core.pipelines.utils.MultiThrowable
import io.github.thibaultbee.streampack.core.pipelines.utils.SourceConfigUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The main pipeline for the streamer.
 *
 * @param context the application context
 * @param withAudio whether the streamer has audio. It will create necessary audio components.
 * @param withVideo whether the streamer has video. It will create necessary video components.
 * @param audioOutputMode the audio output mode. It can be [AudioOutputMode.PUSH] or [AudioOutputMode.CALLBACK]. Only use [AudioOutputMode.CALLBACK] when you have a single output and its implements [IAudioCallbackPipelineOutputInternal]. By default, it is [AudioOutputMode.PUSH].
 * @param surfaceProcessorFactory the factory to create the surface processor
 * @param dispatcherProvider the coroutine dispatcher
 */
open class StreamerPipeline(
    protected val context: Context,
    val withAudio: Boolean = true,
    val withVideo: Boolean = true,
    private val audioOutputMode: AudioOutputMode = AudioOutputMode.PUSH,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    protected val dispatcherProvider: IDispatcherProvider = DispatcherProvider()
) : IWithVideoSource, IWithVideoRotation, IWithAudioSource, IStreamer {
    private val coroutineScope = CoroutineScope(dispatcherProvider.default)
    private val isReleaseRequested = AtomicBoolean(false)

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow = _throwableFlow.asStateFlow()

    // INPUTS
    private val inputMutex = Mutex()
    private val _audioInput = if (withAudio) {
        when (audioOutputMode) {
            AudioOutputMode.PUSH -> AudioInput(
                context,
                AudioInput.PushConfig(::queueAudioFrame),
                dispatcherProvider
            )

            AudioOutputMode.CALLBACK -> AudioInput(
                context,
                AudioInput.CallbackConfig(),
                dispatcherProvider
            )
        }
    } else {
        null
    }
    override val audioInput: IAudioInput? = _audioInput

    private val _videoInput = if (withVideo) {
        VideoInput(context, surfaceProcessorFactory, dispatcherProvider) {
            getOutputSurfaces()
        }
    } else {
        null
    }

    override val videoInput: IVideoInput? = _videoInput

    private val _isStreamingFlow = MutableStateFlow(false)

    /**
     * State flow of the streaming state.
     *
     * It is true if a least one input (audio or video) is streaming.
     */
    override val isStreamingFlow: StateFlow<Boolean> = _isStreamingFlow.asStateFlow()

    // OUTPUTS
    private val outputMapMutex = Mutex()
    private val outputsToJobsMap = hashMapOf<IPipelineOutput, List<Job>>()

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

        withContext(dispatcherProvider.default) {
            val jobs = mutableListOf<Job>()
            safeOutputCall { outputs ->
                jobs += coroutineScope.launch {
                    outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                        .forEach { it.setTargetRotation(rotation) }
                }
            }
            jobs.joinAll()
        }
    }

    init {
        require(withAudio || withVideo) { "At least one of audio or video must be set" }

        _videoInput?.let { input ->
            coroutineScope.launch {
                input.isStreamingFlow.collect { isStreaming ->
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping video streaming state")
                        return@collect
                    }

                    if (!isStreaming) {
                        if (_audioInput?.isStreamingFlow?.value == true) {
                            Logger.i(TAG, "Video input is stopped: stopping video only outputs")
                            // Only stops video only output
                            val jobs = mutableListOf<Job>()
                            safeOutputCall { streamingOutputs ->
                                jobs += stopStreamOutputs(streamingOutputs.filter { !it.key.withAudio && it.key.isStreaming }
                                    .keys)
                            }
                            jobs.joinAll()
                        } else {
                            Logger.i(TAG, "Video input is stopped: stopping all outputs")
                            // Stops all outputs
                            _isStreamingFlow.emit(false)
                            stopStreamOutputs()
                        }
                    }
                }
            }
        }

        _audioInput?.let { input ->
            coroutineScope.launch {
                input.isStreamingFlow.collect { isStreaming ->
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping audio streaming state")
                        return@collect
                    }

                    if (!isStreaming) {
                        if (_videoInput?.isStreamingFlow?.value == true) {
                            Logger.i(TAG, "Audio input is stopped: stopping audio only outputs")
                            // Stops audio only output
                            val jobs = mutableListOf<Job>()
                            safeOutputCall { streamingOutputs ->
                                jobs += stopStreamOutputs(streamingOutputs.filter { !it.key.withVideo && it.key.isStreaming }.keys)
                            }
                            jobs.joinAll()
                        } else {
                            Logger.i(TAG, "Audio input is stopped: stopping all outputs")
                            // Stops all outputs
                            _isStreamingFlow.emit(false)
                            stopStreamOutputs()
                        }
                    }
                }
            }
        }
    }

    private suspend fun setAudioSourceConfig(value: AudioSourceConfig) {
        require(withAudio) { "Do not need to set audio as it is a video only streamer" }
        val input = requireNotNull(_audioInput) { "Audio input is not set" }
        input.setSourceConfig(value)
    }

    private suspend fun queueAudioFrame(frame: RawFrame) {
        safeStreamingOutputCall { streamingOutputs ->
            val audioStreamingOutput =
                streamingOutputs.keys.filterIsInstance<IAudioSyncPipelineOutputInternal>()
            if (audioStreamingOutput.isEmpty()) {
                Logger.w(TAG, "No audio streaming output to process the frame")
                frame.close()
            } else if (audioStreamingOutput.size == 1) {
                try {
                    audioStreamingOutput.first().queueAudioFrame(frame)
                } catch (t: Throwable) {
                    Logger.e(TAG, "Error while queueing audio frame to output: $t")
                }
            } else {
                // Hook to close frame when all outputs have processed it
                var numOfClosed = 0
                val onClosed = { frame: Closeable ->
                    numOfClosed++
                    if (numOfClosed == audioStreamingOutput.size) {
                        frame.close()
                    }
                }
                audioStreamingOutput.forEachIndexed { index, output ->
                    try {
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
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Error while queueing audio frame to output $output: $t")
                    }
                }
            }
        }
    }

    private suspend fun setVideoSourceConfig(value: VideoSourceConfig) {
        require(withVideo) { "Do not need to set video as it is an audio only streamer" }
        val input = requireNotNull(_videoInput) { "Video input is not set" }
        input.setSourceConfig(value)
    }

    // VIDEO PROCESSING
    /**
     * Updates the transformation of the surface output.
     * To be called when the source info provider or [isMirroringRequired] is updated.
     */
    private suspend fun getOutputSurfaces(): List<Triple<SurfaceDescriptor, Boolean, () -> Boolean>> {
        return safeOutputCall { outputs ->
            outputs.keys.filterIsInstance<IVideoSurfacePipelineOutputInternal>()
                .mapNotNull {
                    it.surfaceFlow.value?.let { surfaceDescriptor ->
                        Triple(
                            surfaceDescriptor,
                            isMirroringRequired(),
                            it::isStreaming
                        )
                    }
                }
        }
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
     * @param dispatcherProvider the dispatcher provider for the encoding output
     *
     * @return the [EncodingPipelineOutput] created
     */
    suspend fun createEncodingOutput(
        withAudio: Boolean = this.withAudio,
        withVideo: Boolean = this.withVideo,
        endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
        @RotationValue targetRotation: Int = context.displayRotation
    ): IConfigurableAudioVideoEncodingPipelineOutput {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require(withAudio || withVideo) { "At least one of audio or video must be set" }
        val withAudioCorrected = if (this@StreamerPipeline.withAudio) {
            withAudio
        } else {
            false
        }
        val withVideoCorrected = if (this@StreamerPipeline.withVideo) {
            withVideo
        } else {
            false
        }

        return withContext(dispatcherProvider.default) {
            val output =
                EncodingPipelineOutput(
                    context,
                    withAudioCorrected,
                    withVideoCorrected,
                    endpointFactory,
                    targetRotation,
                    dispatcherProvider
                )
            addOutput(output)
        }
    }

    /**
     * Adds an output.
     *
     * The output must not be already streaming. Also, audio and video source needs to be null.
     *
     * @param output the output to add
     * @return the [output] added (same as input)
     */
    suspend fun <T : IPipelineOutput> addOutput(output: T): T {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        require((output is IVideoPipelineOutputInternal) || (output is IAudioPipelineOutputInternal)) {
            "Output must be an audio or video output"
        }

        return withContext(dispatcherProvider.default) {
            if (safeOutputCall { outputs -> outputs.contains(output) }) {
                throw IllegalStateException("Output $output already added")
            }
            require(!output.isStreaming) { "Output $output is already streaming" }

            try {
                val jobs = addOutputImpl(output, coroutineScope)
                safeOutputCall {
                    outputsToJobsMap[output] = jobs
                }
            } catch (t: Throwable) {
                removeOutput(output)
                try {
                    output.release()
                } catch (t2: Throwable) {
                    Logger.e(
                        TAG,
                        "Error while releasing output $output after a failure to add it",
                        t2
                    )
                }
                throw t
            }
            output
        }
    }

    /**
     * Adds an output implementation.
     *
     * @return the list of jobs created for the output
     */
    private suspend fun addOutputImpl(output: IPipelineOutput, scope: CoroutineScope): List<Job> {
        val jobs = mutableListOf<Job>()
        if (output is IPipelineEventOutputInternal) {
            require(output.streamEventListener == null) { "Output $output already have a listener" }
            output.streamEventListener = object : IPipelineEventOutputInternal.Listener {
                override suspend fun onStartStream() = withContextInputMutex {
                    /**
                     * Verify if the source configuration is still valid with the output configuration.
                     * Another output could have changed the source configuration in the meantime.
                     */
                    if (output.withAudio) {
                        require(withAudio) { "Do not need to set audio as it is a video only streamer" }
                        if (output is IConfigurableAudioPipelineOutput) {
                            val input = requireNotNull(_audioInput) { "Audio input is not set" }
                            val inputSourceConfig =
                                requireNotNull(input.sourceConfigFlow.value) { "Input audio source config is not set" }
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
                            val input = requireNotNull(_videoInput) { "Video input is not set" }
                            val inputSourceConfig =
                                requireNotNull(input.sourceConfigFlow.value) { "Input video source config is not set" }
                            val outputSourceConfig =
                                requireNotNull(output.videoSourceConfigFlow.value) {
                                    "Output video codec config is not set"
                                }
                            require(outputSourceConfig.isCompatibleWith(inputSourceConfig)) { "Output video source config is not compatible with input video source config" }
                        }
                    }

                    // Start stream if it is not already started
                    startInputStreamsUnsafe(output)
                }

                override suspend fun onStopStream() = withContextInputMutex {
                    Logger.i(TAG, "Stopping output $output")
                    stopStreamInputsIfNeededUnsafe(output)
                }
            }
        } else {
            jobs += scope.launch {
                output.isStreamingFlow.collect { isStreaming ->
                    try {
                        if (isStreaming) {
                            if (output !is IPipelineEventOutputInternal) {
                                inputMutex.withLock {
                                    startInputStreamsUnsafe(output)
                                }
                            }
                        } else {
                            if (output !is IPipelineEventOutputInternal) {
                                inputMutex.withLock {
                                    stopStreamInputsIfNeededUnsafe(output)
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        Logger.e(
                            TAG,
                            "Error while handling output $output streaming state change: $t",
                        )
                    }
                }
            }
        }

        if (output is IAudioPipelineOutputInternal) {
            if (withAudio) {
                val audioInput = requireNotNull(_audioInput) { "Audio input is not set" }
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
                    jobs += addConfigurableAudioOutput(output)
                }
            } else {
                Logger.w(TAG, "Pipeline has no audio")
            }
        }

        if (output is IVideoPipelineOutputInternal) {
            if (withVideo) {
                addVideoOutputIfNeeded(output, scope)?.let {
                    jobs += it
                }
                if (output is IConfigurableVideoPipelineOutputInternal) {
                    addConfigurableVideoOutput(output)
                }
            } else {
                Logger.w(TAG, "Pipeline has no video")
            }
        }

        return jobs
    }

    private suspend fun buildAudioSourceConfig(
        currentOutput: IConfigurableAudioPipelineOutputInternal? = null,
        newAudioSourceConfig: AudioSourceConfig? = null
    ): AudioSourceConfig {
        val audioSourceConfigs = safeOutputCall { streamingOutputs ->
            streamingOutputs.keys.filterIsInstance<IAudioPipelineOutputInternal>()
                .filter { it != currentOutput }
                .mapNotNull {
                    (it as? IConfigurableAudioPipelineOutputInternal)?.audioSourceConfigFlow?.value
                }.toMutableSet()
        }
        newAudioSourceConfig?.let { audioSourceConfigs.add(it) }
        return SourceConfigUtils.buildAudioSourceConfig(audioSourceConfigs)
    }

    private suspend fun addAudioAsyncOutputIfNeeded(
        audioInput: AudioInput,
        output: IAudioCallbackPipelineOutputInternal
    ) {
        val numOfAudioOutput = safeOutputCall {
            outputsToJobsMap.keys.filterIsInstance<IAudioPipelineOutputInternal>().size
        }
        require(numOfAudioOutput == 0) {
            "Only one audio output is allowed for sync source but already $numOfAudioOutput found"
        }

        output.audioFrameRequestedListener = audioInput.frameRequestedListener
    }

    private fun addConfigurableAudioOutput(
        output: IConfigurableAudioPipelineOutputInternal
    ): Job {
        require(output.audioSourceConfigFlow.value == null) { "Output $output already have an audio source config" }

        // Catch the config invalidation
        val job = coroutineScope.launch {
            output.audioSourceConfigFlow.drop(1).collect { sourceConfig ->
                if (sourceConfig == null) {
                    withContextInputMutex {
                        try {
                            setAudioSourceConfig(buildAudioSourceConfig(output))
                        } catch (t: Throwable) {
                            Logger.e(
                                TAG,
                                "Error while setting audio source config after invalidation for output $output: $t"
                            )
                        }
                    }
                }
            }
        }

        // Apply future audio source config
        require(output.audioConfigEventListener == null) { "Output $output already have an audio listener" }
        output.audioConfigEventListener =
            object : IConfigurableAudioPipelineOutputInternal.Listener {
                override suspend fun onSetAudioSourceConfig(newAudioSourceConfig: AudioSourceConfig) =
                    withContextInputMutex {
                        setAudioSourceConfig(buildAudioSourceConfig(output, newAudioSourceConfig))
                    }
            }

        return job
    }

    private fun addVideoSurfaceOutputIfNeeded(
        output: IVideoSurfacePipelineOutputInternal, scope: CoroutineScope
    ): Job {
        return scope.launch {
            output.surfaceFlow.runningHistoryNotNull()
                .collect { (previousSurfaceDescriptor, newSurfaceDescriptor) ->
                    Logger.i(TAG, "Surface changed")
                    if (previousSurfaceDescriptor?.surface == newSurfaceDescriptor?.surface) {
                        return@collect
                    }

                    val input = requireNotNull(_videoInput) { "Video input is not set" }

                    previousSurfaceDescriptor?.let {
                        Logger.i(TAG, "Removing previous surface: $previousSurfaceDescriptor")
                        input.removeOutputSurface(it.surface)
                    }
                    if (isReleaseRequested.get()) {
                        Logger.w(TAG, "Pipeline is released, dropping new surface")
                        return@collect
                    }
                    try {
                        newSurfaceDescriptor?.let {
                            Logger.i(TAG, "Adding new surface: $newSurfaceDescriptor")
                            input.addOutputSurface(
                                it,
                                isMirroringRequired(),
                                output::isStreaming
                            )
                        }
                    } catch (t: Throwable) {
                        Logger.e(TAG, "Error while adding new surface: $newSurfaceDescriptor: $t")
                    }
                }
        }
    }

    private fun addVideoOutputIfNeeded(
        output: IVideoPipelineOutputInternal, scope: CoroutineScope
    ): Job? {
        var job: Job? = null
        if (withVideo) {
            when {
                output is IVideoSurfacePipelineOutputInternal -> {
                    job = addVideoSurfaceOutputIfNeeded(output, scope)
                }

                else -> throw NotImplementedError("Output type $output is not supported")
            }
        } else {
            Logger.w(TAG, "Pipeline has no video")
        }
        return job
    }

    private suspend fun buildVideoSourceConfig(
        currentOutput: IConfigurableVideoPipelineOutputInternal? = null,
        newVideoSourceConfig: VideoSourceConfig? = null
    ): VideoSourceConfig {
        val videoSourceConfigs =
            safeOutputCall { streamingOutputs ->
                streamingOutputs.keys.filterIsInstance<IVideoPipelineOutputInternal>()
                    .filterIsInstance<IConfigurableVideoPipelineOutputInternal>()
                    .filter { it != currentOutput }
                    .mapNotNull { it.videoSourceConfigFlow.value }.toMutableSet()
            }
        newVideoSourceConfig?.let { videoSourceConfigs.add(it) }
        return SourceConfigUtils.buildVideoSourceConfig(videoSourceConfigs)
    }

    private fun addConfigurableVideoOutput(
        output: IConfigurableVideoPipelineOutputInternal
    ) {
        require(output.videoSourceConfigFlow.value == null) { "Output $output already have a video source config" }
        // Catch the config invalidation
        coroutineScope.launch {
            output.videoSourceConfigFlow.drop(1).collect { sourceConfig ->
                if (sourceConfig == null) {
                    withContextInputMutex {
                        try {
                            setVideoSourceConfig(buildVideoSourceConfig(output))
                        } catch (t: Throwable) {
                            Logger.e(
                                TAG,
                                "Error while setting video source config after invalidation for output $output: $t"
                            )
                        }
                    }
                }
            }
        }

        // Apply future video source config
        require(output.videoConfigEventListener == null) { "Output $output already have a video listener" }
        output.videoConfigEventListener =
            object : IConfigurableVideoPipelineOutputInternal.Listener {
                override suspend fun onSetVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig) =
                    withContextInputMutex {
                        setVideoSourceConfig(buildVideoSourceConfig(output, newVideoSourceConfig))
                    }
            }
    }

    /**
     * Removes an output.
     *
     * It will stop the stream.
     */
    private fun detachOutput(output: IPipelineOutput) {
        Logger.i(TAG, "Detaching output $output")

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
                _videoInput?.removeOutputSurface(it.surface)
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
    suspend fun removeOutput(output: IPipelineOutput) =
        withContext(dispatcherProvider.default) {
            safeOutputCall { outputs ->
                if (!outputs.contains(output)) {
                    Logger.w(TAG, "Output $output not found")
                    return@safeOutputCall
                }
                outputs[output]?.forEach { it.cancel() }
                outputs.remove(output)
            }

            inputMutex.withLock {
                stopStreamInputsIfNeededUnsafe(output)
            }

            try {
                detachOutput(output)
            } catch (t: Throwable) {
                Logger.w(TAG, "removeOutput: Can't detach output $output: ${t.message}")
            }
        }

    private suspend fun startInputStreamsUnsafe(output: IPipelineOutput) {
        if (output.withAudio) {
            require(_audioInput?.sourceFlow?.value != null) { "At least one audio source must be provided" }
        }
        if (output.withVideo) {
            require(_videoInput?.sourceFlow?.value != null) { "At least one video source must be provided" }
        }
        val isAudioSourceStreaming = if (output.withAudio) {
            val input = requireNotNull(_audioInput)
            val wasStreaming = input.isStreamingFlow.value
            input.startStream()
            wasStreaming
        } else {
            false
        }
        if (output.withVideo) {
            val input = requireNotNull(_videoInput)
            try {
                input.startStream()
            } catch (t: Throwable) {
                // Restore audio source state
                if (!isAudioSourceStreaming) {
                    _audioInput?.stopStream()
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
    override suspend fun startStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }

        withContext(dispatcherProvider.default) {
            val jobs = mutableListOf<Job>()
            val exceptions = mutableListOf<Throwable>()
            safeOutputCall { outputs ->
                outputs.keys.forEach { output ->
                    jobs += coroutineScope.launch {
                        try {
                            output.startStream()
                        } catch (t: Throwable) {
                            exceptions += t
                            Logger.w(
                                TAG,
                                "startStream: Can't start output $output: ${t.message}"
                            )
                        }
                    }
                }
            }
            jobs.joinAll()

            if (exceptions.isNotEmpty()) {
                if (exceptions.size == 1) {
                    throw exceptions.first()
                } else {
                    throw MultiThrowable(exceptions)
                }
            }
        }
    }

    /**
     * Stops all inputs streams.
     */
    private suspend fun stopStreamInputsUnsafe() {
        try {
            try {
                _audioInput?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop audio input: ${t.message}")
            }
            try {
                _videoInput?.stopStream()
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
    private suspend fun stopStreamInputsIfNeededUnsafe(output: IPipelineOutput) {
        // If sources are not streaming, do nothing
        var isAudioSourceStreaming =
            _audioInput != null && _audioInput.isStreamingFlow.value
        var isVideoSourceStreaming =
            _videoInput != null && _videoInput.isStreamingFlow.value
        if (!isAudioSourceStreaming && !isVideoSourceStreaming) {
            return
        }

        val audioJob = _audioInput?.let {
            coroutineScope.launch {
                // Stop audio input if it is the last audio output
                if (output.withAudio && getNumOfAudioStreamingOutputSafe(output) == 0) {
                    Logger.d(TAG, "No more audio output. Stopping audio input.")
                    try {
                        it.stopStream()
                    } catch (t: Throwable) {
                        Logger.w(
                            TAG,
                            "stopStream: Can't stop audio input: ${t.message}"
                        )
                    }
                }
            }
        }

        val videoJob = _videoInput?.let {
            coroutineScope.launch {
                // Stop video input if it is the last video output
                if (output.withVideo && getNumOfVideoStreamingOutputSafe(output) == 0) {
                    Logger.d(TAG, "No more video output. Stopping video input.")
                    try {
                        it.stopStream()
                    } catch (t: Throwable) {
                        Logger.w(
                            TAG,
                            "stopStream: Can't stop video input: ${t.message}"
                        )
                    }
                }
            }
        }

        audioJob?.join()
        videoJob?.join()

        // set isStreamingFlow to false if no more inputs are streaming
        isAudioSourceStreaming =
            _audioInput != null && _audioInput.isStreamingFlow.value
        isVideoSourceStreaming =
            _videoInput != null && _videoInput.isStreamingFlow.value
        if (!isAudioSourceStreaming && !isVideoSourceStreaming) {
            _isStreamingFlow.emit(false)
        }
    }

    private fun stopStreamOutputs(outputs: Set<IPipelineOutput>): List<Job> {
        val jobs = mutableListOf<Job>()
        outputs.forEach { output ->
            jobs += coroutineScope.launch {
                try {
                    output.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop output $output: ${t.message}")
                }
            }
        }
        return jobs
    }

    private suspend fun stopStreamOutputs() {
        val jobs = mutableListOf<Job>()
        safeStreamingOutputCall { outputs ->
            jobs += stopStreamOutputs(outputs.keys)
        }
        jobs.joinAll()
    }

    /**
     * Stops all streams.
     *
     * It stops audio and video sources and calls [IPipelineOutput.stopStream] on all outputs.
     */
    override suspend fun stopStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        withContext(dispatcherProvider.default) {
            inputMutex.withLock {
                stopStreamInputsUnsafe()
            }

            stopStreamOutputs()
        }
    }

    private suspend fun releaseSourcesUnsafe() {
        try {
            _audioInput?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release audio input: ${t.message}")
        }
        try {
            _videoInput?.release()
        } catch (t: Throwable) {
            Logger.w(TAG, "release: Can't release video input: ${t.message}")
        }
    }

    private suspend fun releaseOutputs() {
        safeOutputCall { outputs ->
            outputs.entries.forEach { (output, outputJobs) ->
                outputJobs.forEach { it.cancel() }

                try {
                    detachOutput(output)
                } catch (t: Throwable) {
                    Logger.w(
                        TAG,
                        "release: Can't detach output $output: ${t.message}"
                    )
                }
                try {
                    output.release()
                } catch (t: Throwable) {
                    Logger.w(
                        TAG,
                        "release: Can't release output $output: ${t.message}"
                    )
                }
            }
            outputs.clear()
        }
    }

    /**
     * Releases all resources.
     *
     * It releases the audio and video sources and the processors.
     * It also calls [IPipelineOutput.release] on all outputs.
     */
    override suspend fun release() {
        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
            return
        }
        Logger.d(TAG, "Releasing pipeline")
        withContext(dispatcherProvider.default) {
            // Sources
            inputMutex.withLock {
                try {
                    releaseSourcesUnsafe()
                } catch (t: Throwable) {
                    Logger.w(
                        TAG,
                        "release: Can't release sources: ${t.message}"
                    )
                }
            }
            Logger.d(TAG, "Sources released")

            // Outputs
            releaseOutputs()

            coroutineScope.cancel()
        }
    }

    private suspend fun <T> safeOutputCall(block: suspend (MutableMap<IPipelineOutput, List<Job>>) -> T) =
        withContext(dispatcherProvider.default) {
            outputMapMutex.withLock {
                block(outputsToJobsMap)
            }
        }

    private suspend fun <T> safeStreamingOutputCall(block: suspend (Map<IPipelineOutput, List<Job>>) -> T) =
        safeOutputCall { outputs ->
            val streamingOutputs =
                outputs.filter { it.key.isStreamingFlow.value }
            block(streamingOutputs)
        }

    /**
     * Executes a block with the [coroutineDispatcher] and the [inputMutex] locked.
     */
    private suspend fun <T> withContextInputMutex(block: suspend () -> T): T {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Pipeline is released")
        }
        return withContext(dispatcherProvider.default) {
            inputMutex.withLock {
                block()
            }
        }
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
fun StreamerPipeline.releaseBlocking(dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    runBlocking(dispatcher) {
        release()
    }