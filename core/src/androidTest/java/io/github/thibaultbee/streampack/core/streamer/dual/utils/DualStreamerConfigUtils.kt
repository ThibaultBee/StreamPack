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
package io.github.thibaultbee.streampack.core.streamer.dual.utils

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerAudioCodecConfig
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerAudioConfig
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerVideoCodecConfig
import io.github.thibaultbee.streampack.core.streamers.dual.DualStreamerVideoConfig
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig

object DualStreamerConfigUtils {
    /**
     * Creates a valid audio configuration for test
     *
     * @return a [AudioConfig] for test
     */
    fun defaultAudioConfig() = DualStreamerAudioConfig()

    /**
     * Creates an audio configuration from a [MediaDescriptor] for test
     */
    fun audioConfig(
        firstDescriptor: MediaDescriptor,
        secondDescriptor: MediaDescriptor
    ): DualStreamerAudioConfig {
        val firstAudioConfig = if (firstDescriptor.type.containerType == MediaContainerType.WEBM) {
            DualStreamerAudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
        } else {
            DualStreamerAudioCodecConfig()
        }
        val secondAudioConfig =
            if (secondDescriptor.type.containerType == MediaContainerType.WEBM) {
                DualStreamerAudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
            } else {
                DualStreamerAudioCodecConfig()
            }
        return DualStreamerAudioConfig(firstAudioConfig, secondAudioConfig)
    }

    /**
     * Creates a valid video configuration for test
     *
     * @return a [VideoConfig] for test
     */
    fun defaultVideoConfig(resolution: Size = Size(640, 360)) = DualStreamerVideoConfig(
        VideoConfig(resolution = resolution)
    )

    /**
     * Creates an video configuration from a [MediaDescriptor] for test
     */
    fun videoConfig(
        firstDescriptor: MediaDescriptor,
        secondDescriptor: MediaDescriptor,
        resolution: Size = Size(640, 360)
    ): DualStreamerVideoConfig {
        val firstVideoConfig = if (firstDescriptor.type.containerType == MediaContainerType.WEBM) {
            DualStreamerVideoCodecConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_VP9,
                resolution = resolution
            )
        } else {
            DualStreamerVideoCodecConfig(resolution = resolution)
        }
        val secondVideoConfig =
            if (secondDescriptor.type.containerType == MediaContainerType.WEBM) {
                DualStreamerVideoCodecConfig(
                    mimeType = MediaFormat.MIMETYPE_VIDEO_VP9,
                    resolution = resolution
                )
            } else {
                DualStreamerVideoCodecConfig(resolution = resolution)
            }
        return DualStreamerVideoConfig(
            firstVideoCodecConfig = firstVideoConfig,
            secondVideoCodecConfig = secondVideoConfig
        )
    }
}