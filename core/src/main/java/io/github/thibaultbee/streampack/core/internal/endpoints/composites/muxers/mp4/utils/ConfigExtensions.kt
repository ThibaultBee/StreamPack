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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.utils

import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes.HandlerBox
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes.SoundMediaHeaderBox
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes.TypeMediaHeaderBox
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes.VideoMediaHeaderBox

fun Config.createTypeMediaHeaderBox(): TypeMediaHeaderBox {
    return when (this) {
        is AudioConfig -> SoundMediaHeaderBox()
        is VideoConfig -> VideoMediaHeaderBox()
        else -> throw IllegalArgumentException("Unsupported config")
    }
}

fun Config.createHandlerBox(): HandlerBox {
    return when (this) {
        is AudioConfig -> HandlerBox(HandlerBox.HandlerType.SOUND, "SoundHandler")
        is VideoConfig -> HandlerBox(HandlerBox.HandlerType.VIDEO, "VideoHandler")
        else -> throw IllegalArgumentException("Unsupported config")
    }
}