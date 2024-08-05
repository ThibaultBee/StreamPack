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
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.AudioObjectType
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.ChannelConfiguration
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac.ProgramConfigElement

data class GASpecificConfig(
    val audioObjectType: AudioObjectType,
    val channelConfiguration: ChannelConfiguration,

    val frameLengthFlag: Boolean,
    val dependsOnCoreCoder: Boolean,
    val extensionFlag: Boolean,

    val coreCoderDelay: Short? = null,
    val programConfigElement: ProgramConfigElement? = null,
    val layerNr: Byte? = null,
    val numOfSubFrame: Short? = null,
    val layerLength: Short? = null,
    val aacSectionDataResilienceFlag: Boolean? = null,
    val aacScaleFactorDataResilienceFlag: Boolean? = null,
    val aacSpectralDataResilienceFlag: Boolean? = null,
    val extensionFlag3: Boolean? = null
) : SpecificConfig() {
    override val bitSize =
        3 + if (dependsOnCoreCoder) 14 else 0 + if (channelConfiguration == ChannelConfiguration.SPECIFIC) {
            programConfigElement!!.bitSize
        } else {
            0
        } + if ((audioObjectType == AudioObjectType.AAC_SCALABLE) || (audioObjectType == AudioObjectType.ER_AAC_SCALABLE)) {
            3
        } else {
            0
        } + if (extensionFlag) {
            if (audioObjectType == AudioObjectType.ER_BSAC) {
                16
            } else {
                0
            } + if ((audioObjectType == AudioObjectType.ER_AAC_LC) ||
                (audioObjectType == AudioObjectType.ER_AAC_LTP) ||
                (audioObjectType == AudioObjectType.ER_AAC_SCALABLE) ||
                (audioObjectType == AudioObjectType.ER_AAC_LD)
            ) {
                3
            } else {
                0
            } + 1 // ExtensionFlag3
        } else {
            0
        }

    init {
        if (channelConfiguration == ChannelConfiguration.SPECIFIC) {
            require(programConfigElement != null) { "Program config element must be set" }
        }
    }

    override fun write(output: BitBuffer) {
        TODO("Not yet implemented")
    }

    companion object {
        fun parse(
            reader: BitBuffer,
            channelConfiguration: ChannelConfiguration,
            audioObjectType: AudioObjectType
        ): GASpecificConfig {
            val frameLengthFlag = reader.getBoolean()

            val dependsOnCodeCoder = reader.getBoolean()
            val coreCoderDelay = if (dependsOnCodeCoder) {
                reader.getShort(14)
            } else {
                null
            }

            val extensionFlag = reader.getBoolean()

            val programConfigElement =
                if (channelConfiguration == ChannelConfiguration.SPECIFIC) {
                    ProgramConfigElement.parse(reader)
                } else {
                    null
                }

            val layerNr =
                if ((audioObjectType == AudioObjectType.AAC_SCALABLE) || (audioObjectType == AudioObjectType.ER_AAC_SCALABLE)) {
                    reader.get(3)
                } else {
                    null
                }

            var numOfSubFrame: Short? = null
            var layerLength: Short? = null
            var aacSectionDataResilienceFlag: Boolean? = null
            var aacScaleFactorDataResilienceFlag: Boolean? = null
            var aacSpectralDataResilienceFlag: Boolean? = null
            var extensionFlag3: Boolean? = null

            if (extensionFlag) {
                if (audioObjectType == AudioObjectType.ER_BSAC) {
                    numOfSubFrame = reader.getShort(5)
                    layerLength = reader.getShort(11)
                }

                if ((audioObjectType == AudioObjectType.ER_AAC_LC) ||
                    (audioObjectType == AudioObjectType.ER_AAC_LTP) ||
                    (audioObjectType == AudioObjectType.ER_AAC_SCALABLE) ||
                    (audioObjectType == AudioObjectType.ER_AAC_LD)
                ) {
                    aacSectionDataResilienceFlag = reader.getBoolean()
                    aacScaleFactorDataResilienceFlag = reader.getBoolean()
                    aacSpectralDataResilienceFlag = reader.getBoolean()
                }

                extensionFlag3 = reader.getBoolean()
                if (extensionFlag3) {
                    throw NotImplementedError("Extension flag 3 is not supported")
                }
            }

            return GASpecificConfig(
                audioObjectType,
                channelConfiguration,
                frameLengthFlag,
                dependsOnCodeCoder,
                extensionFlag,
                coreCoderDelay,
                programConfigElement,
                layerNr,
                numOfSubFrame,
                layerLength,
                aacSectionDataResilienceFlag,
                aacScaleFactorDataResilienceFlag,
                aacSpectralDataResilienceFlag,
                extensionFlag3
            )
        }
    }
}