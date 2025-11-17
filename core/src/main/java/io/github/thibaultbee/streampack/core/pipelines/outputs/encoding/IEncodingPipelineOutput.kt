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

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.interfaces.IOpenableStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoRotation
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableAudioPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.IConfigurableVideoPipelineOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineEventOutputInternal
import io.github.thibaultbee.streampack.core.pipelines.outputs.IPipelineOutput
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import kotlinx.coroutines.flow.StateFlow

/**
 * An output component for a streamer.
 */
interface IEncodingPipelineOutput : IPipelineOutput, IOpenableStreamer {
    /**
     * Advanced settings for the endpoint.
     */
    val endpoint: IEndpoint

    /**
     * Adds a bitrate regulator controller to the streamer.
     */
    fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory)

    /**
     * Removes the bitrate regulator controller from the streamer.
     */
    fun removeBitrateRegulatorController()
}

/**
 * A configurable audio output component for a pipeline.
 */
interface IConfigurableAudioEncodingPipelineOutput : IEncodingPipelineOutput,
    IConfigurableAudioPipelineOutput {
    /**
     * The audio configuration flow.
     */
    val audioCodecConfigFlow: StateFlow<AudioCodecConfig?>

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IEncoder?

    /**
     * Configures only audio codec settings.
     *
     * @param audioCodecConfig The audio codec configuration
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setAudioCodecConfig(audioCodecConfig: AudioCodecConfig)

    /**
     * Invalidates current audio codec configuration.
     *
     * You should call [setAudioCodecConfig] after this to set a new configuration.
     */
    suspend fun invalidateAudioCodecConfig()
}

/**
 * A configurable video output component for a pipeline.
 */
interface IConfigurableVideoEncodingPipelineOutput : IEncodingPipelineOutput,
    IConfigurableVideoPipelineOutput, IWithVideoRotation {
    /**
     * The video configuration flow.
     */
    val videoCodecConfigFlow: StateFlow<VideoCodecConfig?>

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IEncoder?

    /**
     * Configures only video codec settings.
     *
     * @param videoCodecConfig The video codec configuration
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setVideoCodecConfig(videoCodecConfig: VideoCodecConfig)

    /**
     * Invalidates current video codec configuration.
     *
     * You should call [setVideoCodecConfig] after this to set a new configuration.
     */
    suspend fun invalidateVideoCodecConfig()
}

interface IConfigurableAudioVideoEncodingPipelineOutput :
    IConfigurableAudioEncodingPipelineOutput, IConfigurableVideoEncodingPipelineOutput

internal interface IEncodingPipelineOutputInternal : IConfigurableAudioVideoEncodingPipelineOutput,
    IConfigurableAudioPipelineOutputInternal,
    IConfigurableVideoPipelineOutputInternal,
    IPipelineEventOutputInternal