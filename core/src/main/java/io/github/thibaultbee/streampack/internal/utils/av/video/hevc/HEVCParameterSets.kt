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
package io.github.thibaultbee.streampack.internal.utils.av.video.hevc

import io.github.thibaultbee.streampack.internal.utils.av.video.ByteBufferBitReader
import io.github.thibaultbee.streampack.internal.utils.av.video.ChromaFormat
import io.github.thibaultbee.streampack.internal.utils.extractRbsp
import java.nio.ByteBuffer

data class SequenceParameterSets(
    val videoParameterSetId: Byte, // 4 bits
    val maxSubLayersMinus1: Byte, // 3 bits
    val temporalIdNestingFlag: Boolean, // 1 bit
    val profileTierLevel: ProfileTierLevel,
    val parameterSetId: Int,
    val chromaFormat: ChromaFormat,
    val picWidthInLumaSamples: Int,
    val picHeightInLumaSamples: Int,
    val bitDepthLumaMinus8: Byte,
    val bitDepthChromaMinus8: Byte
) {
    companion object {
        fun parse(buffer: ByteBuffer): SequenceParameterSets {
            val rbsp = buffer.extractRbsp(2)
            val reader = ByteBufferBitReader(rbsp)
            reader.readNBit(16) // Dropping nal_unit_header: forbidden_zero_bit / nal_unit_type / nuh_layer_id / nuh_temporal_id_plus1

            val videoParameterSetId = reader.readNBit(4).toByte()
            val maxNumSubLayersMinus1 = reader.readNBit(3).toByte()
            val temporalIdNestingFlag = reader.readBoolean()

            val profileTierLevel = ProfileTierLevel.parse(reader, maxNumSubLayersMinus1)
            val parameterSetId = reader.readUE()
            val chromaFormat = ChromaFormat.fromChromaIdc(reader.readUE().toByte())
            if (chromaFormat == ChromaFormat.YUV444) {
                reader.readBoolean()
            }

            val picWidthInLumaSamples = reader.readUE()
            val picHeightInLumaSamples = reader.readUE()

            if (reader.readBoolean()) { // conformance_window_flag
                reader.readUE() // conf_win_left_offset
                reader.readUE() // conf_win_right_offset
                reader.readUE() // conf_win_top_offset
                reader.readUE() // conf_win_bottom_offset
            }

            val bitDepthLumaMinus8 = reader.readUE().toByte()
            val bitDepthChromaMinus8 = reader.readUE().toByte()
            val log2MaxPicOrderCntLsbMinus4 = reader.readUE()

            val subLayerOrderingInfoPresentFlag = reader.readBoolean()
            for (i in (if (subLayerOrderingInfoPresentFlag) 0 else maxNumSubLayersMinus1)..maxNumSubLayersMinus1) {
                reader.readUE() // max_dec_pic_buffering_minus1
                reader.readUE() // max_num_reorder_pics
                reader.readUE() // max_latency_increase_plus1
            }

            reader.readUE() // log2_min_luma_coding_block_size_minus3
            reader.readUE() // log2_diff_max_min_luma_coding_block_size
            reader.readUE() // log2_min_transform_block_size_minus2
            reader.readUE() // log2_diff_max_min_transform_block_size
            reader.readUE() // max_transform_hierarchy_depth_inter
            reader.readUE() // max_transform_hierarchy_depth_intra

            return SequenceParameterSets(
                videoParameterSetId,
                maxNumSubLayersMinus1,
                temporalIdNestingFlag,
                profileTierLevel,
                parameterSetId,
                chromaFormat,
                picWidthInLumaSamples,
                picHeightInLumaSamples,
                bitDepthLumaMinus8,
                bitDepthChromaMinus8
            )
        }
    }
}

data class ProfileTierLevel(
    val generalProfileSpace: Byte, // 2 bits
    val generalTierFlag: Boolean, // 1 bits
    val generalProfileIdc: HEVCProfile, // 5 bits
    val generalProfileCompatibilityFlags: Int, // 32 bits
    val generalConstraintIndicatorFlags: Long, // 48 bits
    val generalLevelIdc: Byte
) {
    companion object {
        fun parse(buffer: ByteBuffer, maxNumSubLayersMinus1: Byte) =
            parse(ByteBufferBitReader(buffer), maxNumSubLayersMinus1)

        fun parse(
            reader: ByteBufferBitReader,
            maxNumSubLayersMinus1: Byte
        ): ProfileTierLevel {
            val generalProfileSpace = reader.readNBit(2).toByte()
            val generalTierFlag = reader.readBoolean()
            val generalProfileIdc = HEVCProfile.fromProfileIdc(reader.readNBit(5).toShort())

            val generalProfileCompatibilityFlags = reader.readNBit(32).toInt()
            val generalConstraintIndicatorFlags = reader.readNBit(48)
            val generalLevelIdc = reader.readNBit(8).toByte()

            val subLayerProfilePresentFlag = mutableListOf<Boolean>()
            val subLayerLevelPresentFlag = mutableListOf<Boolean>()
            for (i in 0 until maxNumSubLayersMinus1) {
                subLayerProfilePresentFlag.add(reader.readBoolean())
                subLayerLevelPresentFlag.add(reader.readBoolean())
            }

            if (maxNumSubLayersMinus1 > 0) {
                for (i in maxNumSubLayersMinus1..8) {
                    reader.readNBit(2) // reserved_zero_2bits
                }
            }

            for (i in 0 until maxNumSubLayersMinus1) {
                if (subLayerProfilePresentFlag[i]) {
                    reader.readNBit(32) // skip
                    reader.readNBit(32) // skip
                    reader.readNBit(24) // skip
                }

                if (subLayerLevelPresentFlag[i]) {
                    reader.readNBit(8) // skip
                }
            }

            return ProfileTierLevel(
                generalProfileSpace,
                generalTierFlag,
                generalProfileIdc,
                generalProfileCompatibilityFlags,
                generalConstraintIndicatorFlags,
                generalLevelIdc
            )
        }
    }
}