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

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac.config.ELDSpecificConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac.config.GASpecificConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac.config.SpecificConfig
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import java.nio.ByteBuffer

data class AudioSpecificConfig(
    val audioObjectType: AudioObjectType,
    val sampleRate: Int,
    val channelConfiguration: ChannelConfiguration,
    val extension: AudioSpecificConfigExtension? = null,
    val specificConfig: SpecificConfig? = null,
    val epConfig: Int? = null
) : BitBufferWriter() {
    override val bitSize =
        getAudioObjectTypeSize(audioObjectType) + getSamplingFrequencySize(sampleRate) + 4 /* channelConfiguration */ + (extension?.bitSize
            ?: 0) + (specificConfig?.bitSize ?: 0) + (epConfig?.let { 2 } ?: 0)

    init {
        if ((audioObjectType == AudioObjectType.PS) || (audioObjectType == AudioObjectType.SBR)) {
            requireNotNull(extension) { "Extension is required for PS or SBR" }
        }

        if (hasSpecificConfig(audioObjectType)) {
            requireNotNull(specificConfig) { "Specific config is required for ${audioObjectType.name}" }
        }

        if (hasErrorProtectionSpecificConfig(audioObjectType)) {
            requireNotNull(epConfig) { "Error protection config is required for ${audioObjectType.name}" }
        }
    }

    override fun write(output: ByteBuffer) {
        val frequencyIndex = SamplingFrequencyIndex.fromSampleRate(sampleRate)
        if (audioObjectType.value <= 0x1F) {
            output.put(
                (audioObjectType.value shl 3)
                        or (frequencyIndex.value shr 1)
            )
        } else {
            throw NotImplementedError("Codec not supported")
        }
        if (frequencyIndex == SamplingFrequencyIndex.EXPLICIT) {
            throw NotImplementedError("Explicit frequency is not supported")
        }
        output.put(
            ((frequencyIndex.value and 0x01) shl 7) or (channelConfiguration.value.toInt() shl 3)
        )
    }

    override fun write(output: BitBuffer) {
        TODO("Not yet implemented")
    }

    companion object {
        private fun getAudioObjectTypeSize(audioObjectType: AudioObjectType): Int {
            return if (audioObjectType.value < 32) {
                5
            } else {
                11
            }
        }

        private fun getAudioObjectType(reader: BitBuffer): AudioObjectType {
            var audioObjectType = reader.getInt(5)
            if (audioObjectType == 0x1F) {
                audioObjectType = 32 + reader.getInt(6)
            }
            return AudioObjectType.fromValue(audioObjectType)
        }

        private fun getSamplingFrequencySize(samplingFrequency: Int): Int {
            val samplingFrequencyIndex = SamplingFrequencyIndex.fromSampleRate(samplingFrequency)
            return if (samplingFrequencyIndex == SamplingFrequencyIndex.EXPLICIT) {
                28
            } else {
                4
            }
        }

        private fun getSamplingFrequency(reader: BitBuffer): Int {
            val samplingFrequencyIndex = reader.getInt(4)
            return if (samplingFrequencyIndex == 0xF) {
                reader.getInt(24)
            } else {
                SamplingFrequencyIndex.fromValue(samplingFrequencyIndex).toSampleRate()
            }
        }

        private fun hasGASpecificConfig(audioObjectType: AudioObjectType): Boolean {
            return when (audioObjectType) {
                AudioObjectType.AAC_MAIN,
                AudioObjectType.AAC_LC,
                AudioObjectType.AAC_SSR,
                AudioObjectType.AAC_LTP,
                AudioObjectType.AAC_SCALABLE,
                AudioObjectType.TWIN_VQ,
                AudioObjectType.ER_AAC_LC,
                AudioObjectType.ER_AAC_LTP,
                AudioObjectType.ER_AAC_SCALABLE,
                AudioObjectType.ER_TWIN_VQ,
                AudioObjectType.ER_BSAC,
                AudioObjectType.ER_AAC_LD -> true
                else -> false
            }
        }

        private fun hasELDSpecificConfig(audioObjectType: AudioObjectType): Boolean {
            return audioObjectType == AudioObjectType.ER_AAC_ELD
        }

        private fun hasSpecificConfig(audioObjectType: AudioObjectType): Boolean {
            when (audioObjectType) {
                AudioObjectType.CELP,
                AudioObjectType.HVXC,
                AudioObjectType.TTSI,
                AudioObjectType.MAIN_SYNTHESIS,
                AudioObjectType.WAVETABLE_SYNTHESIS,
                AudioObjectType.GENERAL_MIDI,
                AudioObjectType.ALGORITHMIC_SYNTHESIS,
                AudioObjectType.ER_CELP,
                AudioObjectType.ER_HVXC,
                AudioObjectType.ER_HILN,
                AudioObjectType.ER_PARAMETRIC,
                AudioObjectType.SSC,
                AudioObjectType.MPEG_SURROUND,
                AudioObjectType.LAYER_1,
                AudioObjectType.LAYER_2,
                AudioObjectType.LAYER_3,
                AudioObjectType.DST,
                AudioObjectType.ALS,
                AudioObjectType.SLS,
                AudioObjectType.SLS_NON_CORE,
                AudioObjectType.ER_AAC_ELD,
                AudioObjectType.SMR_SIMPLE,
                AudioObjectType.SMR_MAIN -> return true
                else -> return hasGASpecificConfig(audioObjectType) || hasELDSpecificConfig(
                    audioObjectType
                )
            }
        }

        private fun hasErrorProtectionSpecificConfig(audioObjectType: AudioObjectType): Boolean {
            return when (audioObjectType) {
                AudioObjectType.ER_AAC_LC,
                AudioObjectType.ER_AAC_LTP,
                AudioObjectType.ER_AAC_SCALABLE,
                AudioObjectType.ER_TWIN_VQ,
                AudioObjectType.ER_BSAC,
                AudioObjectType.ER_AAC_LD,
                AudioObjectType.ER_CELP,
                AudioObjectType.ER_HVXC,
                AudioObjectType.ER_HILN,
                AudioObjectType.ER_PARAMETRIC,
                AudioObjectType.ER_AAC_ELD -> {
                    true
                }
                else -> false
            }
        }

        fun parse(buffer: ByteBuffer): AudioSpecificConfig {
            val bitBuffer = BitBuffer(buffer)
            return parse(bitBuffer)
        }

        fun parse(bitBuffer: BitBuffer): AudioSpecificConfig {
            var audioObjectType = getAudioObjectType(bitBuffer)
            val samplingFrequency = getSamplingFrequency(bitBuffer)
            val channelConfiguration = ChannelConfiguration.fromValue(bitBuffer.getShort(4))

            var extension: AudioSpecificConfigExtension? = null
            if ((audioObjectType == AudioObjectType.SBR) || (audioObjectType == AudioObjectType.PS)) {
                extension = AudioSpecificConfigExtension.parse(bitBuffer)
                audioObjectType = extension.audioObjectType
            }

            val specificConfig = if (hasGASpecificConfig(audioObjectType)) {
                GASpecificConfig.parse(bitBuffer, channelConfiguration, audioObjectType)
            } else if (hasELDSpecificConfig(audioObjectType)) {
                ELDSpecificConfig.parse(bitBuffer, channelConfiguration)
            } else if (hasSpecificConfig(audioObjectType)) {
                TODO("Specific config not yet implemented for ${audioObjectType.name}")
            } else {
                null
            }

            val epConfig = if (hasErrorProtectionSpecificConfig(audioObjectType)) {
                bitBuffer.getInt(2)
            } else {
                null
            }
            if ((epConfig == 2) || (epConfig == 3)) {
                TODO("Error protection config not yet implemented for ${audioObjectType.name}")
            }

            if ((extension != null) && (extension.audioObjectType != AudioObjectType.SBR) && (bitBuffer.bitRemaining >= 16)) {
                val syncExtensionType = bitBuffer.getInt(11)
                if (syncExtensionType == 0x2B7) {
                    val extensionAudioObjectType = getAudioObjectType(bitBuffer)
                    if (extensionAudioObjectType == AudioObjectType.SBR) {
                        val sbrPresentFlag = bitBuffer.getBoolean()
                        if (sbrPresentFlag) {
                            getSamplingFrequency(bitBuffer) // extensionSampleFrequency
                            if (bitBuffer.bitRemaining >= 12) {
                                val syncExtensionType2 = bitBuffer.getInt(11)
                                if (syncExtensionType2 == 0x548) {
                                    bitBuffer.getBoolean() // psPresentFlag
                                }
                            }
                        }
                    } else if (extensionAudioObjectType == AudioObjectType.ER_BSAC) {
                        val sbrPresentFlag = bitBuffer.getBoolean()
                        if (sbrPresentFlag) {
                            getSamplingFrequency(bitBuffer) // extensionSampleFrequency
                        }
                        bitBuffer.getLong(4) // extensionChannelConfiguration
                    }
                }
            }

            return AudioSpecificConfig(
                audioObjectType,
                samplingFrequency,
                channelConfiguration,
                extension,
                specificConfig,
                epConfig
            )
        }

        fun fromMediaFormat(
            format: MediaFormat,
        ): AudioSpecificConfig {
            val audioObjectType = AudioObjectType.fromProfile(
                format.getString(MediaFormat.KEY_MIME)!!,
                format.getInteger(MediaFormat.KEY_AAC_PROFILE)
            )
            val channelConfig =
                ChannelConfiguration.fromChannelCount(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))

            val specificConfig = GASpecificConfig(
                audioObjectType,
                channelConfig,
                frameLengthFlag = false,
                dependsOnCoreCoder = false,
                extensionFlag = false
            )
            return AudioSpecificConfig(
                audioObjectType,
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channelConfig,
                specificConfig = specificConfig
            )
        }

        fun fromAudioConfig(
            config: AudioConfig,
        ): AudioSpecificConfig {
            val audioObjectType = AudioObjectType.fromProfile(config.mimeType, config.profile)
            val channelConfig = ChannelConfiguration.fromChannelConfig(config.channelConfig)

            val specificConfig = GASpecificConfig(
                audioObjectType,
                channelConfig,
                frameLengthFlag = false,
                dependsOnCoreCoder = false,
                extensionFlag = false
            )
            return AudioSpecificConfig(
                audioObjectType,
                config.sampleRate,
                channelConfig,
                specificConfig = specificConfig
            )
        }

        fun writeFromByteBuffer(
            buffer: ByteBuffer,
            decoderSpecificInfo: ByteBuffer,
            audioConfig: AudioConfig
        ) {
            if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                buffer.put(decoderSpecificInfo)
            } else {
                throw NotImplementedError("No support for ${audioConfig.mimeType}")
            }
        }
    }

    data class AudioSpecificConfigExtension(
        val audioObjectType: AudioObjectType,
        val sampleRate: Int,
        val channelConfiguration: ChannelConfiguration?,
        val extensionAudioObjectType: AudioObjectType = AudioObjectType.SBR,
    ) : BitBufferWriter() {
        override val bitSize =
            getSamplingFrequencySize(sampleRate) + getAudioObjectTypeSize(audioObjectType) + (channelConfiguration?.let { 4 }
                ?: 0)

        init {
            if (audioObjectType == AudioObjectType.ER_BSAC) {
                requireNotNull(channelConfiguration) { "Channel configuration is required for ER_BSAC" }
            }
        }

        override fun write(output: BitBuffer) {
            TODO("Not yet implemented")
        }

        companion object {
            fun parse(reader: BitBuffer): AudioSpecificConfigExtension {
                val samplingFrequency = getSamplingFrequency(reader)
                val audioObjectType = getAudioObjectType(reader)
                val channelConfiguration =
                    if (audioObjectType == AudioObjectType.ER_BSAC) {
                        ChannelConfiguration.fromValue(reader.getShort(4))
                    } else {
                        null
                    }

                return AudioSpecificConfigExtension(
                    audioObjectType,
                    samplingFrequency,
                    channelConfiguration
                )
            }
        }
    }
}