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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.hevc

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.video.ChromaFormat
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putLong48
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.removeStartCode
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shr
import io.github.thibaultbee.streampack.core.internal.utils.extensions.startCodeSize
import java.nio.ByteBuffer
import kotlin.experimental.and

data class HEVCDecoderConfigurationRecord(
    private val configurationVersion: Int = 0x01,
    private val generalProfileSpace: Byte,
    private val generalTierFlag: Boolean,
    private val generalProfileIdc: HEVCProfile,
    private val generalProfileCompatibilityFlags: Int,
    private val generalConstraintIndicatorFlags: Long,
    private val generalLevelIdc: Byte,
    private val minSpatialSegmentationIdc: Int = 0,
    private val parallelismType: Byte = 0, // 0 = Unknown
    private val chromaFormat: ChromaFormat = ChromaFormat.YUV420, // Always YUV420 on Android camera
    private val bitDepthLumaMinus8: Byte = 0,
    private val bitDepthChromaMinus8: Byte = 0,
    private val averageFrameRate: Short = 0, // 0 - Unspecified
    private val constantFrameRate: Byte = 0, // 0 - Unknown
    private val numTemporalLayers: Byte = 0, // 0 = Unknown
    private val temporalIdNested: Boolean = false,
    private val lengthSizeMinusOne: Byte = 3,
    private val parameterSets: List<NalUnit>,
) : ByteBufferWriter() {
    override val size: Int = getSize(parameterSets)

    override fun write(output: ByteBuffer) {
        output.put(configurationVersion) // configurationVersion

        // profile_tier_level
        output.put(
            (generalProfileSpace shl 6)
                    or (generalTierFlag shl 5)
                    or (generalProfileIdc.value.toInt())
        )
        output.putInt(generalProfileCompatibilityFlags)
        output.putLong48(generalConstraintIndicatorFlags)
        output.put(generalLevelIdc)

        output.putShort(0xf000 or (minSpatialSegmentationIdc)) // min_spatial_segmentation_idc 12 bits
        output.put(0xfc or (parallelismType.toInt())) // parallelismType 2 bits
        output.put(0xfc or (chromaFormat.value.toInt())) // chromaFormat 2 bits
        output.put(0xf8 or (bitDepthLumaMinus8.toInt())) // bitDepthLumaMinus8 3 bits
        output.put(0xf8 or (bitDepthChromaMinus8.toInt())) // bitDepthChromaMinus8 3 bits

        output.putShort(averageFrameRate) // avgFrameRate
        output.put(
            (constantFrameRate shl 6)
                    or ((numTemporalLayers and 0x7) shl 3)
                    or (temporalIdNested shl 2)
                    or (lengthSizeMinusOne.toInt() and 0x3)
        ) // constantFrameRate 2 bits = 1 for stable / numTemporalLayers 3 bits /  temporalIdNested 1 bit / lengthSizeMinusOne 2 bits

        output.put(parameterSets.size) // numOfArrays

        // It is recommended that the arrays be in the order VPS, SPS, PPS, prefix SEI, suffix SEI.
        val vpsSets = mutableListOf<NalUnit>()
        val spsSets = mutableListOf<NalUnit>()
        val ppsSets = mutableListOf<NalUnit>()
        val prefixSeiSets = mutableListOf<NalUnit>()
        val suffixSeiSets = mutableListOf<NalUnit>()
        val otherSets = mutableListOf<NalUnit>()
        parameterSets.forEach {
            when (it.type) {
                NalUnit.Type.VPS -> vpsSets.add(it)
                NalUnit.Type.SPS -> spsSets.add(it)
                NalUnit.Type.PPS -> ppsSets.add(it)
                NalUnit.Type.PREFIX_SEI -> prefixSeiSets.add(it)
                NalUnit.Type.SUFFIX_SEI -> suffixSeiSets.add(it)
                else -> otherSets.add(it)
            }
        }
        vpsSets.forEach { it.write(output) }
        spsSets.forEach { it.write(output) }
        ppsSets.forEach { it.write(output) }
        prefixSeiSets.forEach { it.write(output) }
        suffixSeiSets.forEach { it.write(output) }
        otherSets.forEach { it.write(output) }
    }

    companion object {
        private const val HEVC_DECODER_CONFIGURATION_RECORD_SIZE = 23
        private const val HEVC_PARAMETER_SET_HEADER_SIZE = 5

        fun fromParameterSets(
            sps: ByteBuffer,
            pps: ByteBuffer,
            vps: ByteBuffer
        ) = fromParameterSets(
            listOf(sps),
            listOf(pps),
            listOf(vps)
        )

        fun fromParameterSets(
            sps: List<ByteBuffer>,
            pps: List<ByteBuffer>,
            vps: List<ByteBuffer>
        ) = fromParameterSets(sps + pps + vps)

        fun fromParameterSets(
            parameterSets: List<ByteBuffer>
        ): HEVCDecoderConfigurationRecord {
            val nalUnitParameterSets = parameterSets.map {
                val nalType = (it[it.startCodeSize] shr 1 and 0x3F).toByte()
                val type = NalUnit.Type.fromValue(nalType)
                NalUnit(type, it)
            }
            // profile_tier_level
            val noStartCodeSps =
                nalUnitParameterSets.first { it.type == NalUnit.Type.SPS }.noStartCodeData
            val parsedSps =
                SequenceParameterSets.parse(noStartCodeSps)
            noStartCodeSps.rewind()
            return HEVCDecoderConfigurationRecord(
                generalProfileSpace = parsedSps.profileTierLevel.generalProfileSpace,
                generalTierFlag = parsedSps.profileTierLevel.generalTierFlag,
                generalProfileIdc = parsedSps.profileTierLevel.generalProfileIdc,
                generalProfileCompatibilityFlags = parsedSps.profileTierLevel.generalProfileCompatibilityFlags,
                generalConstraintIndicatorFlags = parsedSps.profileTierLevel.generalConstraintIndicatorFlags,
                generalLevelIdc = parsedSps.profileTierLevel.generalLevelIdc,
                chromaFormat = parsedSps.chromaFormat,
                bitDepthLumaMinus8 = parsedSps.bitDepthLumaMinus8,
                bitDepthChromaMinus8 = parsedSps.bitDepthChromaMinus8,
                numTemporalLayers = (parsedSps.maxSubLayersMinus1 + 1).toByte(),
                temporalIdNested = parsedSps.temporalIdNesting,
                // TODO get minSpatialSegmentationIdc from VUI
                parameterSets = nalUnitParameterSets
            )
        }

        fun getSize(
            sps: ByteBuffer,
            pps: ByteBuffer,
            vps: ByteBuffer
        ) = getSize(
            listOf(sps),
            listOf(pps),
            listOf(vps)
        )

        fun getSize(
            sps: List<ByteBuffer>,
            pps: List<ByteBuffer>,
            vps: List<ByteBuffer>
        ): Int {
            var size =
                HEVC_DECODER_CONFIGURATION_RECORD_SIZE
            sps.forEach {
                size += it.remaining() - it.startCodeSize + HEVC_PARAMETER_SET_HEADER_SIZE
            }
            pps.forEach {
                size += it.remaining() - it.startCodeSize + HEVC_PARAMETER_SET_HEADER_SIZE
            }
            vps.forEach {
                size += it.remaining() - it.startCodeSize + HEVC_PARAMETER_SET_HEADER_SIZE
            }

            return size
        }

        fun getSize(parameterSets: List<NalUnit>): Int {
            var size =
                HEVC_DECODER_CONFIGURATION_RECORD_SIZE
            parameterSets.forEach {
                size += it.noStartCodeData.remaining() + HEVC_PARAMETER_SET_HEADER_SIZE
            }
            return size
        }
    }

    data class NalUnit(val type: Type, val data: ByteBuffer, val completeness: Boolean = true) {
        val noStartCodeData: ByteBuffer = data.removeStartCode()

        fun write(buffer: ByteBuffer) {
            buffer.put((completeness shl 7) or type.value.toInt()) // array_completeness + reserved 1bit + naluType 6 bytes
            buffer.putShort(1) // numNalus
            buffer.putShort(noStartCodeData.remaining().toShort()) // nalUnitLength
            buffer.put(noStartCodeData)
        }

        enum class Type(val value: Byte) {
            VPS(32),
            SPS(33),
            PPS(34),
            AUD(35),
            EOS(36),
            EOB(37),
            FD(38),
            PREFIX_SEI(39),
            SUFFIX_SEI(40);

            companion object {
                fun fromValue(value: Byte) = entries.first { it.value == value }

            }
        }
    }
}