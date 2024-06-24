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

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.av.video.ChromaFormat
import io.github.thibaultbee.streampack.core.internal.utils.av.video.H26XBitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.extensions.extractRbsp
import java.nio.ByteBuffer

data class SequenceParameterSets(
    val videoParameterSetId: Byte, // 4 bits
    val maxSubLayersMinus1: Byte, // 3 bits
    val temporalIdNesting: Boolean, // 1 bit
    val profileTierLevel: ProfileTierLevel,
    val seqParameterSetId: Int,
    val chromaFormat: ChromaFormat,
    val picWidthInLumaSamples: Int,
    val picHeightInLumaSamples: Int,
    val bitDepthLumaMinus8: Byte,
    val bitDepthChromaMinus8: Byte
) {
    companion object {
        fun parse(buffer: ByteBuffer): SequenceParameterSets {
            val rbsp = buffer.extractRbsp(2)
            val reader = H26XBitBuffer(rbsp)
            reader.getLong(16) // Dropping nal_unit_header: forbidden_zero_bit / nal_unit_type / nuh_layer_id / nuh_temporal_id_plus1

            val videoParameterSetId = reader.get(4)
            val maxNumSubLayersMinus1 = reader.get(3)
            val temporalIdNesting = reader.getBoolean()

            val profileTierLevel = ProfileTierLevel.parse(reader, maxNumSubLayersMinus1)
            val seqParameterSetId = reader.readUE()
            val chromaFormat = ChromaFormat.fromChromaIdc(reader.readUE().toByte())
            if (chromaFormat == ChromaFormat.YUV444) {
                reader.getBoolean()
            }

            val picWidthInLumaSamples = reader.readUE()
            val picHeightInLumaSamples = reader.readUE()

            if (reader.getBoolean()) { // conformance_window_flag
                reader.readUE() // conf_win_left_offset
                reader.readUE() // conf_win_right_offset
                reader.readUE() // conf_win_top_offset
                reader.readUE() // conf_win_bottom_offset
            }

            val bitDepthLumaMinus8 = reader.readUE().toByte()
            val bitDepthChromaMinus8 = reader.readUE().toByte()
            reader.readUE() // log2_max_pic_order_cnt_lsb_minus4

            val subLayerOrderingInfoPresentFlag = reader.getBoolean()
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
                temporalIdNesting,
                profileTierLevel,
                seqParameterSetId,
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
            parse(BitBuffer(buffer), maxNumSubLayersMinus1)

        fun parse(
            reader: BitBuffer,
            maxNumSubLayersMinus1: Byte
        ): ProfileTierLevel {
            val generalProfileSpace = reader.get(2)
            val generalTierFlag = reader.getBoolean()
            val generalProfileIdc = HEVCProfile.entryOf(reader.getShort(5))

            val generalProfileCompatibilityFlags = reader.getInt(32)
            val generalConstraintIndicatorFlags = reader.getLong(48)
            val generalLevelIdc = reader.get(8)

            val subLayerProfilePresentFlag = mutableListOf<Boolean>()
            val subLayerLevelPresentFlag = mutableListOf<Boolean>()
            for (i in 0 until maxNumSubLayersMinus1) {
                subLayerProfilePresentFlag.add(reader.getBoolean())
                subLayerLevelPresentFlag.add(reader.getBoolean())
            }

            if (maxNumSubLayersMinus1 > 0) {
                for (i in maxNumSubLayersMinus1..8) {
                    reader.getLong(2) // reserved_zero_2bits
                }
            }

            for (i in 0 until maxNumSubLayersMinus1) {
                if (subLayerProfilePresentFlag[i]) {
                    reader.getLong(32) // skip
                    reader.getLong(32) // skip
                    reader.getLong(24) // skip
                }

                if (subLayerLevelPresentFlag[i]) {
                    reader.getLong(8) // skip
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