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

import android.media.MediaFormat
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.internal.utils.put
import java.nio.ByteBuffer

/**
 * Audio Decoder-specific information from ESDS
 */
data class AudioDecoderSpecificInformation(
    val audioObjectType: AudioObjectType,
    val sampleRate: Int,
    val channelCount: Int
) {
    companion object {
        fun fromMediaFormat(
            format: MediaFormat,
        ): AudioDecoderSpecificInformation {
            return AudioDecoderSpecificInformation(
                AudioObjectType.fromMimeType(format.getString(MediaFormat.KEY_MIME)!!),
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            )
        }

        fun fromAudioConfig(
            config: AudioConfig,
        ): AudioDecoderSpecificInformation {
            return AudioDecoderSpecificInformation(
                AudioObjectType.fromMimeType(config.mimeType),
                config.sampleRate,
                AudioConfig.getNumberOfChannels(config.channelConfig)
            )
        }
    }

    fun toByteBuffer(): ByteBuffer {
        val decoderSpecificInfo = ByteBuffer.allocate(2)
        val frequencyIndex = SamplingFrequencyIndex.fromSampleRate(sampleRate).value
        if (audioObjectType.value <= 31) {
            decoderSpecificInfo.put(
                (audioObjectType.value shl 3)
                        or (frequencyIndex shr 1)
            )
        } else {
            throw NotImplementedError("Codec not supported")
        }
        if (frequencyIndex == 15) {
            throw NotImplementedError("Explicit frequency is not supported")
        }
        decoderSpecificInfo.put(
            ((frequencyIndex and 0x01) shl 7) or (ChannelConfiguration.fromChannelCount(
                channelCount
            ).value shl 3)
        )

        decoderSpecificInfo.rewind()

        return decoderSpecificInfo
    }
}