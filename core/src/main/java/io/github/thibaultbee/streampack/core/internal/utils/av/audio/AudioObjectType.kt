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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio

import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.io.IOException

enum class AudioObjectType(val value: Int) {
    NULL(0),
    AAC_MAIN(1),
    AAC_LC(2),
    AAC_SSR(3),
    AAC_LTP(4),
    SBR(5),
    AAC_SCALABLE(6),
    TWIN_VQ(7),
    CELP(8),
    HVXC(9),
    TTSI(12),
    MAIN_SYNTHESIS(13),
    WAVETABLE_SYNTHESIS(14),
    GENERAL_MIDI(15),
    ALGORITHMIC_SYNTHESIS(16),
    ER_AAC_LC(17),
    ER_AAC_LTP(19),
    ER_AAC_SCALABLE(20),
    ER_TWIN_VQ(21),
    ER_BSAC(22),
    ER_AAC_LD(23),
    ER_CELP(24),
    ER_HVXC(25),
    ER_HILN(26),
    ER_PARAMETRIC(27),
    SSC(28),
    PS(29),
    MPEG_SURROUND(30),
    LAYER_1(32),
    LAYER_2(33),
    LAYER_3(34),
    DST(35),
    ALS(36),
    SLS(37),
    SLS_NON_CORE(38),
    ER_AAC_ELD(39),
    SMR_SIMPLE(40),
    SMR_MAIN(41),
    USAC_NO_SBR(42),
    SAOC(43),
    LD_MPEG_SURROUND(44),
    USAC(45);

    val extensionAudioObjectType: Byte
        get() = if ((this == SBR) || (this == PS)) {
            5
        } else {
            0
        }

    val sbrPresentFlag: Boolean
        get() = (this == SBR) || (this == PS)

    val psPresentFlag: Boolean
        get() = (this == SBR)

    companion object {
        fun fromValue(value: Int): AudioObjectType {
            return entries.first { it.value == value }
        }

        fun fromProfile(mimeType: String, profile: Int) = when (mimeType) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                when (profile) {
                    MediaCodecInfo.CodecProfileLevel.AACObjectMain -> AAC_MAIN
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC -> AAC_LC
                    MediaCodecInfo.CodecProfileLevel.AACObjectSSR -> AAC_SSR
                    MediaCodecInfo.CodecProfileLevel.AACObjectLTP -> AAC_LTP
                    MediaCodecInfo.CodecProfileLevel.AACObjectHE -> SBR
                    MediaCodecInfo.CodecProfileLevel.AACObjectScalable -> AAC_SCALABLE
                    MediaCodecInfo.CodecProfileLevel.AACObjectERLC -> ER_AAC_LC
                    MediaCodecInfo.CodecProfileLevel.AACObjectLD -> ER_AAC_LD
                    MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS -> PS
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD -> ER_AAC_ELD
                    MediaCodecInfo.CodecProfileLevel.AACObjectERScalable -> ER_AAC_SCALABLE
                    MediaCodecInfo.CodecProfileLevel.AACObjectXHE -> USAC_NO_SBR
                    else -> throw IOException("Unsupported AAC profile: $profile")
                }
            }
            else -> throw IOException("MimeType is not supported: $mimeType")
        }
    }
}