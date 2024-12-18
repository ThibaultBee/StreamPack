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

enum class TransferCharacteristics(val value: Int) {
    BT709(1),
    UNSPECIFIED(2),
    GAMMA22(4),
    GAMMA28(5),
    SMPTE_170M(6),
    SMPTE_240M(7),
    LINEAR(8),
    LOG(9),
    LOG_SQRT(10),
    IEC61966_2_4(11),
    BT1361_0(12),
    IEC61966_2_1(13),
    BT2020_10(14),
    BT2020_12(15),
    SMPTE_ST2084(16),
    SMPTE_ST428(17),
    ARIB_STD_B67(18);

    companion object {
        fun fromValue(value: Int) =
            entries.first { it.value == value }

        fun fromColorTransfer(colorTransfer: Int): TransferCharacteristics {
            return when (colorTransfer) {
                MediaFormat.COLOR_TRANSFER_LINEAR -> LINEAR
                MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> SMPTE_170M
                MediaFormat.COLOR_TRANSFER_ST2084 -> SMPTE_ST2084
                MediaFormat.COLOR_TRANSFER_HLG -> ARIB_STD_B67
                else -> throw IllegalArgumentException("Unsupported color transfer: $colorTransfer")
            }
        }
    }
}