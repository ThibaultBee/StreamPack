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
package io.github.thibaultbee.streampack.core.utils

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig

object ConfigurationUtils {
    /**
     * Creates a valid audio configuration for test
     *
     * @return a [AudioCodecConfig] for test
     */
    fun defaultAudioConfig() = AudioCodecConfig()

    /**
     * Creates an audio configuration from a [MediaDescriptor] for test
     */
    fun audioConfig(descriptor: MediaDescriptor): AudioConfig {
        return if (descriptor.type.containerType == MediaContainerType.WEBM) {
            AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
        } else {
            defaultAudioConfig()
        }
    }

    /**
     * Creates a valid video configuration for test
     *
     * @return a [VideoCodecConfig] for test
     */
    fun defaultVideoConfig() = VideoCodecConfig(
        resolution = Size(640, 360)
    )

    /**
     * Creates an video configuration from a [MediaDescriptor] for test
     */
    fun videoConfig(descriptor: MediaDescriptor): VideoConfig {
        return if (descriptor.type.containerType == MediaContainerType.WEBM) {
            VideoCodecConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_VP9)
        } else {
            defaultVideoConfig()
        }
    }
}