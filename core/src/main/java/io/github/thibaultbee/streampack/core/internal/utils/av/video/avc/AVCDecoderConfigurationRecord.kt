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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.avc

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.video.ChromaFormat
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.removeStartCode
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.core.internal.utils.extensions.startCodeSize
import java.nio.ByteBuffer

data class AVCDecoderConfigurationRecord(
    private val configurationVersion: Int = 0x01,
    private val profileIdc: Byte,
    private val profileCompatibility: Byte,
    private val levelIdc: Byte,
    private val chromaFormat: ChromaFormat = ChromaFormat.YUV420, // Always YUV420 on Android camera
    private val sps: List<ByteBuffer>,
    private val pps: List<ByteBuffer>
) : ByteBufferWriter() {
    private val spsNoStartCode: List<ByteBuffer> = sps.map { it.removeStartCode() }
    private val ppsNoStartCode: List<ByteBuffer> = pps.map { it.removeStartCode() }

    override val size: Int = getSize(spsNoStartCode, ppsNoStartCode)

    override fun write(output: ByteBuffer) {
        output.put(configurationVersion) // configurationVersion
        output.put(profileIdc) // AVCProfileIndication
        output.put(profileCompatibility) // profile_compatibility
        output.put(levelIdc) // AVCLevelIndication

        output.put(0xff.toByte()) // 6 bits reserved + lengthSizeMinusOne - 4 bytes
        output.put((0b111.toByte() shl 5) or (spsNoStartCode.size)) // 3 bits reserved + numOfSequenceParameterSets - 5 bytes
        spsNoStartCode.forEach {
            output.putShort(it.remaining()) // sequenceParameterSetLength
            output.put(it)
        }

        output.put(ppsNoStartCode.size) // numOfPictureParameterSets
        ppsNoStartCode.forEach {
            output.putShort(it.remaining()) // sequenceParameterSetLength
            output.put(it)
        }

        if ((profileIdc == 100.toByte()) || (profileIdc == 110.toByte()) || (profileIdc == 122.toByte()) || (profileIdc == 144.toByte())) {
            output.put(
                (0b111111 shl 2) // reserved
                        or chromaFormat.value.toInt() // chroma_format
            )
            output.put(
                (0b11111 shl 3) // reserved
                        or 0 // bit_depth_luma_minus8
            )
            output.put(
                (0b11111 shl 3) // reserved
                        or 0 // bit_depth_chroma_minus8
            )
            output.put(0)
        }
    }

    companion object {
        private const val AVC_DECODER_CONFIGURATION_RECORD_SIZE = 7

        fun fromParameterSets(
            sps: ByteBuffer,
            pps: ByteBuffer
        ) = fromParameterSets(listOf(sps), listOf(pps))

        fun fromParameterSets(
            sps: List<ByteBuffer>,
            pps: List<ByteBuffer>
        ): AVCDecoderConfigurationRecord {
            val spsNoStartCode = sps.map { it.removeStartCode() }
            val ppsNoStartCode = pps.map { it.removeStartCode() }
            val profileIdc: Byte = spsNoStartCode[0].get(1)
            val profileCompatibility = spsNoStartCode[0].get(2)
            val levelIdc = spsNoStartCode[0].get(3)
            return AVCDecoderConfigurationRecord(
                profileIdc = profileIdc,
                profileCompatibility = profileCompatibility,
                levelIdc = levelIdc,
                sps = spsNoStartCode,
                pps = ppsNoStartCode
            )
        }

        fun getSize(sps: ByteBuffer, pps: ByteBuffer) = getSize(listOf(sps), listOf(pps))

        fun getSize(sps: List<ByteBuffer>, pps: List<ByteBuffer>): Int {
            var size =
                AVC_DECODER_CONFIGURATION_RECORD_SIZE
            sps.forEach {
                size += 2 + it.remaining() - it.startCodeSize
            }
            pps.forEach {
                size += 2 + it.remaining() - it.startCodeSize
            }
            val spsStartCodeSize = sps[0].startCodeSize
            val profileIdc = sps[0].get(spsStartCodeSize + 1).toInt()
            if ((profileIdc == 100) || (profileIdc == 110) || (profileIdc == 122) || (profileIdc == 144)) {
                size += 4
            }
            return size
        }
    }
}