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
package io.github.thibaultbee.streampack.core.streamer.single.utils

import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaContainerType
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig

object SingleStreamerConfigUtils {
    /**
     * Creates a valid audio configuration for test
     *
     * @return a [AudioConfig] for test
     */
    fun defaultAudioConfig() = AudioConfig()

    /**
     * Creates an audio configuration from a [MediaDescriptor] for test
     */
    fun audioConfig(descriptor: MediaDescriptor): AudioConfig {
        return if ((descriptor.type.containerType == MediaContainerType.WEBM)
            || (descriptor.type.containerType == MediaContainerType.OGG)
        ) {
            AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
        } else {
            defaultAudioConfig()
        }
    }

    /**
     * Creates a valid video configuration for test
     *
     * @return a [VideoConfig] for test
     */
    fun defaultVideoConfig(resolution: Size = Size(640, 360)) = VideoConfig(
        resolution = resolution
    )

    /**
     * Creates an video configuration from a [MediaDescriptor] for test
     */
    fun videoConfig(descriptor: MediaDescriptor, resolution: Size = Size(640, 360)): VideoConfig {
        return if (descriptor.type.containerType == MediaContainerType.WEBM) {
            VideoCodecConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_VP9, resolution = resolution)
        } else {
            defaultVideoConfig()
        }
    }
}