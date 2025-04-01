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
package io.github.thibaultbee.streampack.core.streamers.single

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.interfaces.IOpenableStreamer
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.IAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * The single streamer audio configuration.
 */
typealias AudioConfig = AudioCodecConfig

/**
 * The single streamer video configuration.
 */
typealias VideoConfig = VideoCodecConfig

/**
 * A single Streamer that is agnostic to the underlying implementation (either with coroutines or callbacks).
 */
interface ISingleStreamer : IOpenableStreamer {
    /**
     * Advanced settings for the endpoint.
     */
    val endpoint: IEndpoint

    /**
     * Configuration information
     */
    val info: IConfigurationInfo

    /**
     * Gets configuration information
     */
    fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo

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
 * An audio single Streamer
 */
interface IAudioSingleStreamer : IAudioStreamer<AudioConfig> {
    /**
     * The audio configuration flow.
     */
    val audioConfigFlow: StateFlow<AudioConfig?>

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IEncoder?
}

/**
 * A video single streamer.
 */
interface IVideoSingleStreamer : IVideoStreamer<VideoConfig> {
    /**
     * The video configuration flow.
     */
    val videoConfigFlow: StateFlow<VideoConfig?>

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IEncoder?
}

