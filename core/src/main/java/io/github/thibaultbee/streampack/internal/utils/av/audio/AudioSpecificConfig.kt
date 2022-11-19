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
package io.github.thibaultbee.streampack.internal.utils.av.audio

import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.internal.utils.av.ByteBufferBitReader
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import java.nio.ByteBuffer

data class AudioSpecificConfig(
    val audioObjectType: AudioObjectType,
    val sampleRate: Int,
    val channelConfiguration: ChannelConfiguration,
    val extension: AudioSpecificConfigExtension? = null
) {
    init {
        if ((audioObjectType == AudioObjectType.PS) || (audioObjectType == AudioObjectType.SBR)) {
            requireNotNull(extension)
        }
    }

    companion object {
        private fun getAudioObjectType(reader: ByteBufferBitReader): AudioObjectType {
            var audioObjectType = reader.readNBit(5).toInt()
            if (audioObjectType == 0x1F) {
                audioObjectType += reader.readNBit(6).toInt()
            }
            return AudioObjectType.fromValue(audioObjectType)
        }

        fun parse(buffer: ByteBuffer): AudioSpecificConfig {
            val reader = ByteBufferBitReader(buffer)
            val audioObjectType = getAudioObjectType(reader)
            val samplingFrequencyIndex = reader.readNBit(4).toInt()
            val samplingFrequency = if (samplingFrequencyIndex == 0xF) {
                reader.readNBit(24).toInt()
            } else {
                SamplingFrequencyIndex.fromValue(samplingFrequencyIndex).toSampleRate()
            }
            val channelConfiguration = ChannelConfiguration.fromValue(reader.readNBit(4).toShort())

            val extension =
                if ((audioObjectType == AudioObjectType.SBR) || (audioObjectType == AudioObjectType.PS)) {
                    AudioSpecificConfigExtension.parse(reader)
                } else {
                    null
                }

            // TODO: parse all information

            return AudioSpecificConfig(
                audioObjectType,
                samplingFrequency,
                channelConfiguration,
                extension
            )
        }

        fun fromMediaFormat(
            format: MediaFormat,
        ): AudioSpecificConfig {
            return AudioSpecificConfig(
                AudioObjectType.fromMimeType(format.getString(MediaFormat.KEY_MIME)!!),
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                ChannelConfiguration.fromChannelCount(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
            )
        }

        fun fromAudioConfig(
            config: AudioConfig,
        ): AudioSpecificConfig {
            return AudioSpecificConfig(
                AudioObjectType.fromMimeType(config.mimeType),
                config.sampleRate,
                ChannelConfiguration.fromChannelConfig(config.channelConfig)
            )
        }

        fun parseAndWrite(buffer: ByteBuffer, esds: ByteBuffer, audioConfig: AudioConfig) {
            if (audioConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                buffer.put(esds)
            } else {
                throw NotImplementedError("No support for ${audioConfig.mimeType}")
            }
        }
    }

    fun toByteBuffer(): ByteBuffer {
        val decoderSpecificInfo = ByteBuffer.allocate(2)
        val frequencyIndex = SamplingFrequencyIndex.fromSampleRate(sampleRate)
        if (audioObjectType.value <= 0x1F) {
            decoderSpecificInfo.put(
                (audioObjectType.value shl 3)
                        or (frequencyIndex.value shr 1)
            )
        } else {
            throw NotImplementedError("Codec not supported")
        }
        if (frequencyIndex == SamplingFrequencyIndex.EXPLICIT) {
            throw NotImplementedError("Explicit frequency is not supported")
        }
        decoderSpecificInfo.put(
            ((frequencyIndex.value and 0x01) shl 7) or channelConfiguration.value.toInt() shl 3
        )

        decoderSpecificInfo.rewind()

        return decoderSpecificInfo
    }


    data class AudioSpecificConfigExtension(
        val audioObjectType: AudioObjectType,
        val sampleRate: Int,
        val channelConfiguration: ChannelConfiguration?
    ) {
        companion object {
            fun parse(reader: ByteBufferBitReader): AudioSpecificConfigExtension {
                val extensionSamplingFrequencyIndex = reader.readNBit(4).toInt()
                val extensionSamplingFrequency = if (extensionSamplingFrequencyIndex == 0xF) {
                    reader.readNBit(24).toInt()
                } else {
                    SamplingFrequencyIndex.fromValue(extensionSamplingFrequencyIndex).toSampleRate()
                }
                val extensionAudioObjectType = getAudioObjectType(reader)
                val extensionChannelConfiguration =
                    if (extensionAudioObjectType == AudioObjectType.ER_BSAC) {
                        ChannelConfiguration.fromValue(reader.readNBit(4).toShort())
                    } else {
                        null
                    }

                return AudioSpecificConfigExtension(
                    extensionAudioObjectType,
                    extensionSamplingFrequency,
                    extensionChannelConfiguration
                )
            }
        }
    }
}
