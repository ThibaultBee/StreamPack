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
import com.github.thibaultbee.streampack.internal.bitbuffer.BitBuffer
import java.nio.ByteBuffer

class Adts(private val format: MediaFormat, private val payloadLength: Int) {

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
        val protectionAbsent = true // No CRC protection
        val adts = BitBuffer.allocate(if (protectionAbsent) 7 else 9) // 56: 7 Bytes - 48: 9 Bytes

        adts.put(0xFFF, 12)
        adts.put(0, 1) // MPEG-4
        adts.put(0, 2) // Layer
        adts.put(protectionAbsent)

        adts.put(1, 2) // AAC-LC = 2 - minus 1
        val samplingFrequencyIndex =
            samplingFrequencyIndex(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        adts.put(samplingFrequencyIndex, 4)
        adts.put(0, 1) // Private bit
        val channelConfiguration =
            channelConfiguration(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        adts.put(channelConfiguration, 3)
        adts.put(0, 1) // originality
        adts.put(0, 1) // home

        adts.put(0, 1) // copyright it bit
        adts.put(0, 1) // copyright id start

        val frameLength = payloadLength + if (protectionAbsent) 7 else 9
        adts.put(frameLength, 13)

        adts.put(0x7FF, 11) // Buffer fullness 0x7FF for variable bitrate
        adts.put(0b00, 2)

        return adts.toByteBuffer()
    }
}