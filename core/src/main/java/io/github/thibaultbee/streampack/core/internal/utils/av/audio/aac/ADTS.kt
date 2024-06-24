/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.ChannelConfiguration
import io.github.thibaultbee.streampack.core.internal.utils.av.audio.SamplingFrequencyIndex
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toInt
import java.nio.ByteBuffer

data class ADTS(
    val protectionAbsent: Boolean, // No CRC protection
    val sampleRate: Int,
    val channelCount: Int,
    val payloadLength: Int
) : ByteBufferWriter() {
    override val size = if (protectionAbsent) 7 else 9

    override fun write(output: ByteBuffer) {
        output.putShort(
            (0xFFF shl 4)
                    or (0b000 shl 1) // MPEG-4 + Layer
                    or (protectionAbsent.toInt())
        )

        val samplingFrequencyIndex =
            SamplingFrequencyIndex.fromSampleRate(sampleRate).value
        val channelConfiguration =
            ChannelConfiguration.fromChannelCount(channelCount).value
        val frameLength = payloadLength + if (protectionAbsent) 7 else 9
        output.putInt(
            (1 shl 30) // AAC-LC = 2 - minus 1
                    or (samplingFrequencyIndex shl 26)
                    // 0 - Private bit
                    or (channelConfiguration.toInt() shl 22)
                    // 0 - originality
                    // 0 - home
                    // 0 - copyright id bit
                    // 0 - copyright id start
                    or (frameLength shl 5)
                    or (0b11111) // Buffer fullness 0x7FF for variable bitrate
        )
        output.put(0b11111100) // Buffer fullness 0x7FF for variable bitrate
    }

    companion object {
        fun fromMediaFormat(format: MediaFormat, payloadLength: Int): ADTS {
            return ADTS(
                true,
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                payloadLength
            )
        }

        fun fromAudioConfig(config: AudioConfig, payloadLength: Int): ADTS {
            return ADTS(
                true,
                config.sampleRate,
                AudioConfig.getNumberOfChannels(config.channelConfig),
                payloadLength
            )
        }
    }
}