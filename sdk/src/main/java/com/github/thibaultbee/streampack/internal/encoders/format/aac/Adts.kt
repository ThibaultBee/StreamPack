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
package com.github.thibaultbee.streampack.internal.encoders.format.aac

import android.media.MediaFormat
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.put
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.putShort
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.toInt
import java.nio.ByteBuffer

data class Adts(
    private val protectionAbsent: Boolean, // No CRC protection
    private val sampleRate: Int,
    private val channelCount: Int,
    private val payloadLength: Int
) {
    constructor(format: MediaFormat, payloadLength: Int) : this(
        true,
        format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        payloadLength
    )

    private fun samplingFrequencyIndex(samplingFrequency: Int): Int {
        return when (samplingFrequency) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            7350 -> 12
            else -> 15
        }
    }

    private fun channelConfiguration(channelCount: Int): Int {
        return when (channelCount) {
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 6
            8 -> 7
            else -> 0
        }
    }

    fun toByteBuffer(): ByteBuffer {
        val adts = ByteBuffer.allocate(if (protectionAbsent) 7 else 9) // 56: 7 Bytes - 48: 9 Bytes

        adts.putShort(
            (0xFFF shl 4)
                    or (0b000 shl 1) // MPEG-4 + Layer
                    or (protectionAbsent.toInt())
        )

        val samplingFrequencyIndex =
            samplingFrequencyIndex(sampleRate)
        val channelConfiguration =
            channelConfiguration(channelCount)
        val frameLength = payloadLength + if (protectionAbsent) 7 else 9
        adts.putInt(
            (1 shl 30) // AAC-LC = 2 - minus 1
                    or (samplingFrequencyIndex shl 26)
                    // 0 - Private bit
                    or (channelConfiguration shl 22)
                    // 0 - originality
                    // 0 - home
                    // 0 - copyright id bit
                    // 0 - copyright id start
                    or (frameLength shl 5)
                    or (0b11111) // Buffer fullness 0x7FF for variable bitrate
        )
        adts.put(0b11111100) // Buffer fullness 0x7FF for variable bitrate

        adts.rewind()
        return adts
    }
}