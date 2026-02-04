/*
 * Copyright (C) 2026 Thibault B.
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
import io.github.thibaultbee.streampack.core.elements.processing.video.DefaultSurfaceProcessorFactory
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal.Factory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isCompatibleWith
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline.AudioOutputMode
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.utils.MultiThrowable
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking


/**
 * A class that handles 2 outputs.
 *
 * For example, you can use it to live stream and record simultaneously.
 *
 * @param context the application context
 * @param withAudio `true` to capture audio. It can't be changed after instantiation.
 * @param withVideo `true` to capture video. It can't be changed after instantiation.
 * @param firstEndpointFactory the [IEndpointInternal] implementation of the first output. By default, it is a [DynamicEndpoint].
 * @param secondEndpointFactory the [IEndpointInternal] implementation of the second output. By default, it is a [DynamicEndpoint].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation to use to create the video processor. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
internal class DualStreamerImpl(
    private val context: Context,
    withAudio: Boolean = true,
    withVideo: Boolean = true,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider(),
) : IDualStreamer, IAudioDualStreamer, IVideoDualStreamer {
    private val coroutineScope = CoroutineScope(dispatcherProvider.default)

    private val pipeline = StreamerPipeline(
        context,
        withAudio,
        withVideo,
        AudioOutputMode.PUSH,
        surfaceProcessorFactory,
        dispatcherProvider
    )

    private val firstPipelineOutput: IEncodingPipelineOutputInternal =
        runBlocking(dispatcherProvider.default) {
            pipeline.createEncodingOutput(
                withAudio, withVideo, firstEndpointFactory, defaultRotation
            ) as IEncodingPipelineOutputInternal
        }


    /**
     * First output of the streamer.
     */
    override val first = firstPipelineOutput as IConfigurableAudioVideoEncodingPipelineOutput

    private val secondPipelineOutput: IEncodingPipelineOutputInternal =
        runBlocking(dispatcherProvider.default) {
            pipeline.createEncodingOutput(
                withAudio, withVideo, secondEndpointFactory, defaultRotation
            ) as IEncodingPipelineOutputInternal
        }

    /**
     * Second output of the streamer.
     */
    override val second = secondPipelineOutput as IConfigurableAudioVideoEncodingPipelineOutput

    override val throwableFlow: StateFlow<Throwable?> = merge(
        pipeline.throwableFlow,
        firstPipelineOutput.throwableFlow,
        secondPipelineOutput.throwableFlow
    ).stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        null
    )

    /**
     * Whether any of the output is opening.
     */
    override val isOpenFlow: StateFlow<Boolean> = combineTransform(
        firstPipelineOutput.isOpenFlow, secondPipelineOutput.isOpenFlow
    ) { isOpens ->
        emit(isOpens.any { it })
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )

    /**
     * Whether any of the output is streaming.
     */
    override val isStreamingFlow: StateFlow<Boolean> = pipeline.isStreamingFlow

    /**
     * Closes the outputs.
     * Same as calling [first.close] and [second.close].
     */
    override suspend fun close() {
        firstPipelineOutput.close()
        secondPipelineOutput.close()
    }

    // SOURCES
    override val audioInput = pipeline.audioInput

    // PROCESSORS
    override val videoInput = pipeline.videoInput

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) {
        pipeline.setTargetRotation(rotation)
    }

    /**
     * Sets audio configuration.
     *
     * It is a shortcut for [IConfigurableAudioVideoEncodingPipelineOutput.setAudioCodecConfig].
     *
     * @param audioConfig the audio configuration to set
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: DualStreamerAudioConfig) {
        val throwables = mutableListOf<Throwable>()

        val firstAudioCodecConfig = firstPipelineOutput.audioCodecConfigFlow.value
        if ((firstAudioCodecConfig != null) && (!firstAudioCodecConfig.isCompatibleWith(audioConfig.firstAudioConfig))) {
            firstPipelineOutput.invalidateAudioCodecConfig()
        }
        val secondAudioCodecConfig = secondPipelineOutput.audioCodecConfigFlow.value
        if ((secondAudioCodecConfig != null) && (!secondAudioCodecConfig.isCompatibleWith(
                audioConfig.secondAudioConfig
            ))
        ) {
            secondPipelineOutput.invalidateAudioCodecConfig()
        }

        try {
            firstPipelineOutput.setAudioCodecConfig(audioConfig.firstAudioConfig)
        } catch (t: Throwable) {
            throwables += t
        }
        try {
            audioConfig.secondAudioConfig.let { secondPipelineOutput.setAudioCodecConfig(it) }
        } catch (t: Throwable) {
            throwables += t
        }
        if (throwables.isNotEmpty()) {
            if (throwables.size == 1) {
                throw throwables.first()
            } else {
                throw MultiThrowable(throwables)
            }
        }
    }

    /**
     * Sets video configuration.
     *
     * It is a shortcut for [IConfigurableAudioVideoEncodingPipelineOutput.setVideoCodecConfig].
     * To only set video configuration for a specific output, use [first.setVideoCodecConfig] or
     * [second.setVideoCodecConfig] outputs.
     * In that case, you call [first.setVideoCodecConfig] or [second.setVideoCodecConfig] explicitly,
     * make sure that the frame rate for both configurations is the same.
     *
     * @param videoConfig the video configuration to set
     */
    override suspend fun setVideoConfig(videoConfig: DualStreamerVideoConfig) {
        val throwables = mutableListOf<Throwable>()

        val firstVideoCodecConfig = firstPipelineOutput.videoCodecConfigFlow.value
        if ((firstVideoCodecConfig != null) && (!firstVideoCodecConfig.isCompatibleWith(videoConfig.firstVideoConfig))) {
            firstPipelineOutput.invalidateVideoCodecConfig()
        }

        val secondVideoCodecConfig = secondPipelineOutput.videoCodecConfigFlow.value
        if ((secondVideoCodecConfig != null) && (!secondVideoCodecConfig.isCompatibleWith(
                videoConfig.secondVideoConfig
            ))
        ) {
            secondPipelineOutput.invalidateVideoCodecConfig()
        }

        try {
            firstPipelineOutput.setVideoCodecConfig(videoConfig.firstVideoConfig)
        } catch (t: Throwable) {
            throwables += t
        }
        try {
            secondPipelineOutput.setVideoCodecConfig(videoConfig.secondVideoConfig)
        } catch (t: Throwable) {
            throwables += t
        }
        if (throwables.isNotEmpty()) {
            if (throwables.size == 1) {
                throw throwables.first()
            } else {
                throw MultiThrowable(throwables)
            }
        }
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
     * @see [DualStreamer.release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun setConfig(
        audioConfig: DualStreamerAudioConfig, videoConfig: DualStreamerVideoConfig
    ) {
        setAudioConfig(audioConfig)
        setVideoConfig(videoConfig)
    }


    override suspend fun startStream() = pipeline.startStream()

    override suspend fun stopStream() = pipeline.stopStream()

    override suspend fun release() {
        pipeline.release()
        coroutineScope.cancel()
    }
}