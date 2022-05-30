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
package io.github.thibaultbee.streampack.internal.utils.av.video

import io.github.thibaultbee.streampack.internal.utils.put
import io.github.thibaultbee.streampack.internal.utils.putShort
import io.github.thibaultbee.streampack.internal.utils.shl
import java.nio.ByteBuffer

class AVCDecoderConfigurationRecord(
    private val profileIdc: Byte,
    private val profileCompatibility: Byte,
    private val levelIdc: Byte,
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>
) {
    private val spsNoStartCode: List<ByteBuffer> = sps.map { it.removeStartCode() }
    private val ppsNoStartCode: List<ByteBuffer> = pps.map { it.removeStartCode() }

    val size: Int = getSize(spsNoStartCode, ppsNoStartCode)

    fun write(buffer: ByteBuffer) {
        buffer.put(0x01) // configurationVersion
        buffer.put(profileIdc) // AVCProfileIndication
        buffer.put(profileCompatibility) // profile_compatibility
        buffer.put(levelIdc) // AVCLevelIndication

        buffer.put(0xff.toByte()) // 6 bits reserved + lengthSizeMinusOne - 4 bytes
        buffer.put((0b111.toByte() shl 5) or (spsNoStartCode.size)) // 3 bits reserved + numOfSequenceParameterSets - 5 bytes
        spsNoStartCode.forEach {
            buffer.putShort(it.remaining()) // sequenceParameterSetLength
            buffer.put(it)
        }

        buffer.put(ppsNoStartCode.size) // numOfPictureParameterSets
        ppsNoStartCode.forEach {
            buffer.putShort(it.remaining()) // sequenceParameterSetLength
            buffer.put(it)
        }

        if ((profileIdc == 100.toByte()) || (profileIdc == 110.toByte()) || (profileIdc == 122.toByte()) || (profileIdc == 144.toByte())) {
            buffer.put(
                (0b111111 shl 2) // reserved
                        or (ColorFormat.YUV420.value) // chroma_format: always YUV420 on Android
            )
            buffer.put(
                (0b11111 shl 3) // reserved
                        or 0 // bit_depth_luma_minus8
            )
            buffer.put(
                (0b11111 shl 3) // reserved
                        or 0 // bit_depth_chroma_minus8
            )
            buffer.put(0)
        }
    }

    companion object {
        private const val AVC_DECODER_CONFIGURATION_RECORD_SIZE = 11

        fun fromSPSAndPPS(
            sps: ByteBuffer,
            pps: ByteBuffer
        ) = fromSPSAndPPS(listOf(sps), listOf(pps))

        fun fromSPSAndPPS(
            sps: List<ByteBuffer>,
            pps: List<ByteBuffer>
        ): AVCDecoderConfigurationRecord {
            val spsNoStartCode = sps.map { it.removeStartCode() }
            val ppsNoStartCode = pps.map { it.removeStartCode() }
            val profileIdc: Byte = spsNoStartCode[0].get(1)
            val profileCompatibility = spsNoStartCode[0].get(2)
            val levelIdc = spsNoStartCode[0].get(3)
            return AVCDecoderConfigurationRecord(
                profileIdc,
                profileCompatibility,
                levelIdc,
                spsNoStartCode,
                ppsNoStartCode
            )
        }

        fun getSize(sps: ByteBuffer, pps: ByteBuffer) = getSize(listOf(sps), listOf(pps))

        fun getSize(sps: List<ByteBuffer>, pps: List<ByteBuffer>): Int {
            var size =
                AVC_DECODER_CONFIGURATION_RECORD_SIZE
            sps.forEach {
                size += it.remaining() - it.getStartCodeSize()
            }
            pps.forEach {
                size += it.remaining() - it.getStartCodeSize()
            }
            val spsStartCodeSize = sps[0].getStartCodeSize()
            val profileIdc = sps[0].get(spsStartCodeSize + 1).toInt()
            if ((profileIdc == 100) || (profileIdc == 110) || (profileIdc == 122) || (profileIdc == 144)) {
                size += 4
            }
            return size
        }
    }

    enum class ColorFormat(val value: Int) {
        YUV400(0),
        YUV420(1),
        YUV422(2),
        YUV444(3)
    }
}