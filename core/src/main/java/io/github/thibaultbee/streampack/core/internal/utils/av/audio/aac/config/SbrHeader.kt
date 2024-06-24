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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac.config

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBufferWriter

data class SbrHeader(
    val bsAmpRes: Boolean,
    val bsStartFreq: Byte,
    val bsStopFreq: Byte,
    val bsXoverBand: Byte,
    val bsHeaderExtra1: Boolean,
    val bsHeaderExtra2: Boolean,
    val bsFreqScale: Byte? = null,
    val bsAlterScale: Boolean? = null,
    val bsNoiseBands: Byte? = null,
    val bsLimiterBands: Byte? = null,
    val bsLimiterGains: Byte? = null,
    val bsInterpolFreq: Boolean? = null,
    val bsSmoothingMode: Boolean? = null
) : BitBufferWriter() {
    override val bitSize = 0

    override fun write(output: BitBuffer) {
        TODO("Not yet implemented")
    }

    companion object {
        fun parse(reader: BitBuffer): SbrHeader {
            val bsAmpRes = reader.getBoolean()
            val bsStartFreq = reader.get(4)

            val bsStopFreq = reader.get(4)

            val bsXoverBand = reader.get(3)

            reader.getLong(2) // bsReserved
            val bsHeaderExtra1 = reader.getBoolean()
            val bsHeaderExtra2 = reader.getBoolean()

            var bsFreqScale: Byte? = null
            var bsAlterScale: Boolean? = null
            var bsNoiseBands: Byte? = null
            if (bsHeaderExtra1) {
                bsFreqScale = reader.get(2)
                bsAlterScale = reader.getBoolean()
                bsNoiseBands = reader.get(2)
            }

            var bsLimiterBands: Byte? = null
            var bsLimiterGains: Byte? = null
            var bsInterpolFreq: Boolean? = null
            var bsSmoothingMode: Boolean? = null
            if (bsHeaderExtra2) {
                bsLimiterBands = reader.get(2)
                bsLimiterGains = reader.get(2)
                bsInterpolFreq = reader.getBoolean()
                bsSmoothingMode = reader.getBoolean()
            }

            return SbrHeader(
                bsAmpRes,
                bsStartFreq,
                bsStopFreq,
                bsXoverBand,
                bsHeaderExtra1,
                bsHeaderExtra2,
                bsFreqScale,
                bsAlterScale,
                bsNoiseBands,
                bsLimiterBands,
                bsLimiterGains,
                bsInterpolFreq,
                bsSmoothingMode
            )
        }
    }
}