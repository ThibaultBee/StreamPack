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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

import android.media.MediaFormat

enum class ColorPrimaries(val value: Int) {
    BT709(1),
    UNSPECIFIED(2),
    BT470M(4),
    BT470BG(5),
    SMPTE170M(6),
    SMPTE240M(7),
    FILM(8),
    BT2020(9),
    SMPTE428(10),
    SMPTEST428_1(10),
    SMPTE431(11),
    SMPTE432(12);

    companion object {
        fun fromValue(value: Int) =
            entries.first { it.value == value }

        fun fromColorStandard(colorStandard: Int): ColorPrimaries {
            return when (colorStandard) {
                MediaFormat.COLOR_STANDARD_BT709 -> BT709
                MediaFormat.COLOR_STANDARD_BT601_PAL -> BT470BG
                MediaFormat.COLOR_STANDARD_BT601_NTSC -> SMPTE170M
                MediaFormat.COLOR_STANDARD_BT2020 -> BT2020
                else -> throw IllegalArgumentException("Unsupported color standard: $colorStandard")
            }
        }
    }
}