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
package com.github.thibaultbee.streampack.internal.utils.av.audio

enum class SamplingFrequencyIndex(val value: Int) {
    F_96000HZ(0),
    F_88200HZ(1),
    F_64000HZ(2),
    F_48000HZ(3),
    F_44100HZ(4),
    F_32000HZ(5),
    F_24000HZ(6),
    F_22050HZ(7),
    F_16000HZ(8),
    F_12000HZ(9),
    F_11025HZ(10),
    F_8000HZ(11),
    F_7350HZ(12),
    EXPLICIT(15);

    companion object {
        fun fromSampleRate(sampleRate: Int) = when (sampleRate) {
            96000 -> F_96000HZ
            88200 -> F_88200HZ
            64000 -> F_64000HZ
            48000 -> F_48000HZ
            44100 -> F_44100HZ
            32000 -> F_32000HZ
            24000 -> F_24000HZ
            22050 -> F_22050HZ
            16000 -> F_16000HZ
            12000 -> F_12000HZ
            11025 -> F_11025HZ
            8000 -> F_8000HZ
            7350 -> F_7350HZ
            else -> EXPLICIT
        }
    }
}