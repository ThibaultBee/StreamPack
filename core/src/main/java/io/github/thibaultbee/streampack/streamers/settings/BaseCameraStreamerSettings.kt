/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.streamers.settings

import io.github.thibaultbee.streampack.internal.encoders.AudioMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.encoders.VideoMediaCodecEncoder
import io.github.thibaultbee.streampack.internal.sources.IAudioSource
import io.github.thibaultbee.streampack.internal.sources.camera.CameraSource
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IAudioSettings
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IBaseCameraStreamerSettings
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IVideoSettings
import io.github.thibaultbee.streampack.utils.CameraSettings


/**
 * Get the base camera settings ie all settings available for [BaseCameraStreamer].
 */
class BaseCameraStreamerSettings(
    private val cameraSource: CameraSource,
    audioSettings: IAudioSettings,
    videoSettings: IVideoSettings
) : BaseStreamerSettings(audioSettings, videoSettings), IBaseCameraStreamerSettings {

    constructor(
        audioSource: IAudioSource?,
        cameraSource: CameraSource,
        audioEncoder: AudioMediaCodecEncoder?,
        videoEncoder: VideoMediaCodecEncoder?
    ) : this(
        cameraSource, BaseStreamerAudioSettings(audioSource, audioEncoder),
        BaseStreamerVideoSettings(videoEncoder)
    )

    /**
     * Get the camera settings (focus, zoom,...).
     */
    override val camera: CameraSettings
        get() = cameraSource.settings
}