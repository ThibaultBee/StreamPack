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

enum class MatrixCoefficients(val value: Int) {
    IDENTITY(0),
    BT709(1),
    UNSPECIFIED(2),
    RESERVED(3),
    FCC(4),
    BT470BG(5),
    SMPTE_170M(6),
    SMPTE_240M(7),
    YCGCO(8),
    BT2020_NCL(9),
    BT2020_CL(10),
    SMPTE_2085(11),
    CHROMAT_CL(12),
    CHROMAT_NCL(14);


    companion object {
        fun fromValue(value: Int) =
            entries.first { it.value == value }

        fun fromColorStandard(colorStandard: Int): MatrixCoefficients {
            return when (colorStandard) {
                MediaFormat.COLOR_STANDARD_BT709 -> BT709
                MediaFormat.COLOR_STANDARD_BT601_PAL -> BT470BG
                MediaFormat.COLOR_STANDARD_BT601_NTSC -> SMPTE_170M
                MediaFormat.COLOR_STANDARD_BT2020 -> BT2020_NCL
                else -> throw IllegalArgumentException("Unsupported color standard: $colorStandard")
            }
        }
    }
}