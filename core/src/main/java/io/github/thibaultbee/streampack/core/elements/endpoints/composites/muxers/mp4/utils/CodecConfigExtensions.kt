/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.utils

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.boxes.HandlerBox
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.boxes.SoundMediaHeaderBox
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.boxes.TypeMediaHeaderBox
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.boxes.VideoMediaHeaderBox

fun CodecConfig.createTypeMediaHeaderBox(): TypeMediaHeaderBox {
    return when (this) {
        is AudioCodecConfig -> SoundMediaHeaderBox()
        is VideoCodecConfig -> VideoMediaHeaderBox()
        else -> throw IllegalArgumentException("Unsupported config")
    }
}

fun CodecConfig.createHandlerBox(): HandlerBox {
    return when (this) {
        is AudioCodecConfig -> HandlerBox(HandlerBox.HandlerType.SOUND, "SoundHandler")
        is VideoCodecConfig -> HandlerBox(HandlerBox.HandlerType.VIDEO, "VideoHandler")
        else -> throw IllegalArgumentException("Unsupported config")
    }
}