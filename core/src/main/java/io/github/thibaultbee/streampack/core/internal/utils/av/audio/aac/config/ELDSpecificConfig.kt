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

import io.github.thibaultbee.streampack.core.internal.utils.av.audio.ChannelConfiguration
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBufferWriter

data class ELDSpecificConfig(
    val channelConfiguration: ChannelConfiguration,

    val frameLengthFlag: Boolean,
    val aacSectionDataResilienceFlag: Boolean,
    val aacScaleFactorDataResilienceFlag: Boolean,
    val aacSpectralDataResilienceFlag: Boolean,
    val lbSbrPresentFlag: Boolean,
    val lbSbrSamplingRate: Boolean? = null,
    val lbSbrCrcFlag: Boolean? = null,
    val lbSbrHeader: LdSbrHeader? = null
) : SpecificConfig() {
    override val bitSize =
        5 + if (lbSbrPresentFlag) {
            2 + lbSbrHeader!!.size
        } else {
            0
        }

    init {
        if (lbSbrPresentFlag) {
            requireNotNull(lbSbrSamplingRate) { "Sampling rate factor must not be null" }
            requireNotNull(lbSbrCrcFlag) { "CRC must not be null" }
            requireNotNull(lbSbrHeader) { "SBR header must not be null" }
        }
    }

    override fun write(output: BitBuffer) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val ELDEXT_TERM = 0b0000

        fun parse(
            reader: BitBuffer,
            channelConfiguration: ChannelConfiguration
        ): ELDSpecificConfig {
            val frameLengthFlag = reader.getBoolean()
            val aacSectionDataResilienceFlag = reader.getBoolean()
            val aacScaleFactorDataResilienceFlag = reader.getBoolean()
            val aacSpectralDataResilienceFlag = reader.getBoolean()

            val ldSbrPresentFlag = reader.getBoolean()
            var ldSbrSamplingRate: Boolean? = null
            var ldSbrCrcFlag: Boolean? = null
            var lbSbrHeader: LdSbrHeader? = null
            if (ldSbrPresentFlag) {
                ldSbrSamplingRate = reader.getBoolean()
                ldSbrCrcFlag = reader.getBoolean()
                lbSbrHeader = LdSbrHeader.parse(reader, channelConfiguration)
            }

            var eldExtType = reader.getInt(4)
            while (eldExtType != ELDEXT_TERM) {
                val eldExtLen = reader.getInt(4)
                var len = eldExtLen

                val eldExtLenAdd = if (eldExtLen == 15) {
                    reader.getInt(8)
                } else {
                    0
                }
                len += eldExtLenAdd

                val eldExtLenAddAdd = if (eldExtLenAdd == 255) {
                    reader.getInt(16)
                } else {
                    0
                }
                len += eldExtLenAddAdd

                /*
                when (eldExtType) {
                    else -> {
                    */
                for (i in 0 until len) {
                    reader.getLong(8) // other_byte
                }
                // }
                // }
                eldExtType = reader.getInt(4)
            }

            return ELDSpecificConfig(
                channelConfiguration,
                frameLengthFlag,
                aacSectionDataResilienceFlag,
                aacScaleFactorDataResilienceFlag,
                aacSpectralDataResilienceFlag,
                ldSbrPresentFlag,
                ldSbrSamplingRate,
                ldSbrCrcFlag,
                lbSbrHeader
            )
        }
    }

    class LdSbrHeader(
        private val sbrHeaders: List<SbrHeader>
    ) : BitBufferWriter() {
        override val bitSize = (5 + sbrHeaders.size * 8) * Byte.SIZE_BITS

        override fun write(output: BitBuffer) {
            sbrHeaders.forEach { it.write(output) }
        }

        companion object {
            fun parse(reader: BitBuffer, channelConfiguration: ChannelConfiguration): LdSbrHeader {
                val sbrHeader = mutableListOf<SbrHeader>()
                val numSbrHeader = when (channelConfiguration) {
                    ChannelConfiguration.CHANNEL_1,
                    ChannelConfiguration.CHANNEL_2 -> {
                        1
                    }

                    ChannelConfiguration.CHANNEL_3 -> {
                        2
                    }

                    ChannelConfiguration.CHANNEL_4,
                    ChannelConfiguration.CHANNEL_5,
                    ChannelConfiguration.CHANNEL_6 -> {
                        3
                    }

                    ChannelConfiguration.CHANNEL_8 -> {
                        4
                    }

                    else -> {
                        0
                    }
                }
                for (i in 0 until numSbrHeader) {
                    sbrHeader.add(SbrHeader.parse(reader))
                }

                return LdSbrHeader(sbrHeader)
            }
        }
    }
}