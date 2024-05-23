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
package io.github.thibaultbee.streampack.streamers.interfaces

import android.Manifest
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.encoders.IPublicEncoder
import io.github.thibaultbee.streampack.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.internal.sources.audio.IPublicAudioSource
import io.github.thibaultbee.streampack.internal.sources.video.IPublicVideoSource
import io.github.thibaultbee.streampack.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.streamers.DefaultStreamer
import io.github.thibaultbee.streampack.streamers.infos.IConfigurationInfo

/**
 * A Streamer that is agnostic to the underlying implementation (either with coroutines or callbacks).
 */
interface IStreamer {
    /**
     * Advanced settings for the audio source.
     */
    val audioSource: IPublicAudioSource?

    /**
     * Advanced settings for the audio encoder.
     */
    val audioEncoder: IPublicEncoder?

    /**
     * Advanced settings for the video source.
     */
    val videoSource: IPublicVideoSource?

    /**
     * Advanced settings for the video encoder.
     */
    val videoEncoder: IPublicEncoder?

    /**
     * Advanced settings for the endpoint.
     */
    val endpoint: IPublicEndpoint

    /**
     * Configuration information
     */
    val info: IConfigurationInfo

    /**
     * Gets configuration information
     */
    fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo

    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configure(audioConfig: AudioConfig)

    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    fun configure(videoConfig: VideoConfig)

    /**
     * Configures both video and audio settings.
     * It is the first method to call after a [DefaultStreamer] instantiation.
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
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun configure(audioConfig: AudioConfig, videoConfig: VideoConfig) {
        configure(audioConfig)
        configure(videoConfig)
    }

    /**
     * Clean and reset the streamer.
     *
     * @see [configure]
     */
    fun release()

    /**
     * Adds a bitrate regulator controller to the streamer.
     */
    fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory)

    /**
     * Removes the bitrate regulator controller from the streamer.
     */
    fun removeBitrateRegulatorController()
}