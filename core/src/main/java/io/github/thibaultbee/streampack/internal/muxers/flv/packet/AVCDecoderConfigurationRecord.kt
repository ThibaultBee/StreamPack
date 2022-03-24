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
package io.github.thibaultbee.streampack.internal.muxers.flv.packet

import io.github.thibaultbee.streampack.internal.utils.av.video.getStartCodeSize
import io.github.thibaultbee.streampack.internal.utils.av.video.removeStartCode
import io.github.thibaultbee.streampack.internal.utils.put
import io.github.thibaultbee.streampack.internal.utils.putShort
import java.nio.ByteBuffer

class AVCDecoderConfigurationRecord(sps: ByteBuffer, pps: ByteBuffer) {
    private val spsNoStartCode = sps.removeStartCode()
    private val ppsNoStartCode = pps.removeStartCode()

    fun write(buffer: ByteBuffer) {
        buffer.put(0x01) // configurationVersion
        val profileIdc = spsNoStartCode.get(1).toInt()
        buffer.put(profileIdc) // AVCProfileIndication
        buffer.put(spsNoStartCode.get(2)) // profile_compatibility
        buffer.put(spsNoStartCode.get(3)) // AVCLevelIndication

        buffer.put(0xff.toByte()) // 6 bits reserved + lengthSizeMinusOne - 4 bytes
        buffer.put(0xe1.toByte()) // 3 bits reserved + numOfSequenceParameterSets - 5 bytes

        buffer.putShort(spsNoStartCode.remaining()) // sequenceParameterSetLength
        buffer.put(spsNoStartCode)
        buffer.put(0x01) // numOfPictureParameterSets

        buffer.putShort(ppsNoStartCode.remaining()) // sequenceParameterSetLength
        buffer.put(ppsNoStartCode)

        if ((profileIdc == 100) || (profileIdc == 110) || (profileIdc == 122) || (profileIdc == 144)) {
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

        fun getSize(sps: ByteBuffer, pps: ByteBuffer): Int {
            val spsStartCodeSize = sps.getStartCodeSize()
            var size =
                AVC_DECODER_CONFIGURATION_RECORD_SIZE + sps.remaining() - spsStartCodeSize + pps.remaining() - pps.getStartCodeSize()
            val profileIdc = sps.get(spsStartCodeSize + 1).toInt()
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