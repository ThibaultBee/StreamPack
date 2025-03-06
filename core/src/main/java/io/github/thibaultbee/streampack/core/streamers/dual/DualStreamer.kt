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
package io.github.thibaultbee.streampack.core.streamers.dual

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.combineStates
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * A class that handles 2 audio and video output.
 *
 * For example, you can use it to live stream and record simultaneously.
 *
 * @param context the application context
 * @param hasAudio [Boolean.true] to capture audio. It can't be changed after instantiation.
 * @param hasVideo [Boolean.true] to capture video. It can't be changed after instantiation.
 * @param firstEndpointInternalFactory the [IEndpointInternal] implementation of the first output. By default, it is a [DynamicEndpoint].
 * @param secondEndpointInternalFactory the [IEndpointInternal] implementation of the second output. By default, it is a [DynamicEndpoint].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class DualStreamer(
    protected val context: Context,
    val hasAudio: Boolean = true,
    val hasVideo: Boolean = true,
    firstEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : ICoroutineDualStreamer, ICoroutineAudioDualStreamer, ICoroutineVideoDualStreamer {
    private val pipeline = StreamerPipeline(
        context,
        hasAudio,
        hasVideo
    )

    private val firstPipelineOutput: IEncodingPipelineOutputInternal = runBlocking {
        pipeline.addOutput(
            firstEndpointInternalFactory,
            defaultRotation
        ) as IEncodingPipelineOutputInternal
    }

    /**
     * First output of the streamer.
     */
    val first = firstPipelineOutput as IConfigurableEncodingPipelineOutput

    private val secondPipelineOutput: IEncodingPipelineOutputInternal = runBlocking {
        pipeline.addOutput(
            secondEndpointInternalFactory,
            defaultRotation
        ) as IEncodingPipelineOutputInternal
    }

    /**
     * Second output of the streamer.
     */
    val second = secondPipelineOutput as IConfigurableEncodingPipelineOutput

    override val throwableFlow: StateFlow<Throwable?> =
        combineStates(
            pipeline.throwableFlow,
            firstPipelineOutput.throwableFlow,
            secondPipelineOutput.throwableFlow
        ) { throwableArray ->
            throwableArray[0] ?: throwableArray[1] ?: throwableArray[2]
        }

    /**
     * Whether any of the output is opening.
     */
    override val isOpenFlow: StateFlow<Boolean> = combineStates(
        firstPipelineOutput.isOpenFlow,
        secondPipelineOutput.isOpenFlow
    ) { isOpeningArray ->
        isOpeningArray[0] || isOpeningArray[1]
    }

    /**
     * Whether any of the output is streaming.
     */
    override val isStreamingFlow: StateFlow<Boolean> = combineStates(
        pipeline.isStreamingFlow,
        firstPipelineOutput.isStreamingFlow,
        secondPipelineOutput.isStreamingFlow
    ) { isStreamingArray ->
        isStreamingArray[0] && (isStreamingArray[1] || isStreamingArray[2])
    }

    /**
     * Closes the outputs.
     * Same as calling [first.close] and [second.close].
     */
    override suspend fun close() {
        firstPipelineOutput.close()
        secondPipelineOutput.close()
    }

    // SOURCES
    override val audioSource: IAudioSource?
        get() = pipeline.audioSourceFlow.value

    open suspend fun setAudioSource(audioSource: IAudioSourceInternal) {
        pipeline.setAudioSource(audioSource)
    }
    
    override val videoSource: IVideoSource?
        get() = pipeline.videoSourceFlow.value

    open suspend fun setVideoSource(videoSource: IVideoSourceInternal) {
        pipeline.setVideoSource(videoSource)
    }

    // PROCESSORS
    override val audioProcessor: IAudioFrameProcessor?
        get() = pipeline.audioProcessor

    /**
     * The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    var targetRotation: Int
        @RotationValue get() = pipeline.targetRotation
        set(@RotationValue newTargetRotation) {
            pipeline.targetRotation = newTargetRotation
        }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: DualStreamerAudioConfig) {
        var throwable: Throwable? = null

        try {
            firstPipelineOutput.setAudioCodecConfig(audioConfig.firstAudioConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        try {
            audioConfig.secondAudioConfig.let { secondPipelineOutput.setAudioCodecConfig(it) }
        } catch (e: Throwable) {
            throwable = e
        }
        throwable?.let { throw it }
    }

    /**
     * Sets video configuration.
     *
     * It is a shortcut for [IEncodingPipelineOutput.setVideoCodecConfig].
     * To only set video configuration for a specific output, use [first.setVideoCodecConfig] or
     * [second.setVideoCodecConfig] outputs.
     * In that case, you call [first.setVideoCodecConfig] or [second.setVideoCodecConfig] explicitly,
     * make sure that the frame rate for both configurations is the same.
     *
     * @param videoConfig the video configuration to set
     */
    override suspend fun setVideoConfig(videoConfig: DualStreamerVideoConfig) {
        var throwable: Throwable? = null
        try {
            firstPipelineOutput.setVideoCodecConfig(videoConfig.firstVideoConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        try {
            secondPipelineOutput.setVideoCodecConfig(videoConfig.secondVideoConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        throwable?.let { throw it }
    }

    /**
     * Configures both video and audio settings.
     *
     * It must be call when both stream and audio and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     * @see [ISingleStreamer.release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun setConfig(
        audioConfig: DualStreamerAudioConfig,
        videoConfig: DualStreamerVideoConfig
    ) {
        setAudioConfig(audioConfig)
        setVideoConfig(videoConfig)
    }


    override suspend fun startStream() =
        pipeline.startStream()

    override suspend fun stopStream() =
        pipeline.stopStream()

    override suspend fun release() =
        pipeline.release()
}