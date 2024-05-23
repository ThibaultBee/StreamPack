/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.internal.utils.av.video.ChromaFormat
import io.github.thibaultbee.streampack.internal.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.av.video.hevc.HEVCProfile
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class HEVCConfigurationBoxTest {
    @Test
    fun `write valid hvcC test`() {
        val vps = ByteBuffer.wrap(
            byteArrayOf(
                0x40,
                0x01,
                0x0C,
                0x01,
                0xFF.toByte(),
                0xFF.toByte(),
                0x01,
                0x60,
                0x00,
                0x00,
                0x03,
                0x00,
                0x80.toByte(),
                0x00,
                0x00,
                0x03,
                0x00,
                0x00,
                0x03,
                0x00,
                0x78,
                0x9D.toByte(),
                0xC0.toByte(),
                0x90.toByte()
            )
        )
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x42,
                0x01,
                0x01,
                0x01,
                0x60,
                0x00,
                0x00,
                0x03,
                0x00,
                0x80.toByte(),
                0x00,
                0x00,
                0x03,
                0x00,
                0x00,
                0x03,
                0x00,
                0x78,
                0xA0.toByte(),
                0x03,
                0xC0.toByte(),
                0x80.toByte(),
                0x32,
                0x16,
                0x59,
                0xDE.toByte(),
                0x49,
                0x1B,
                0x6B,
                0x80.toByte(),
                0x40,
                0x00,
                0x00,
                0xFA.toByte(),
                0x00,
                0x00,
                0x17,
                0x70,
                0x02
            )
        )
        val pps = ByteBuffer.wrap(
            byteArrayOf(
                0x44,
                0x01,
                0xC1.toByte(),
                0x73,
                0xD1.toByte(),
                0x89.toByte()
            )
        )
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("hvcC.box")
        val hvcC = HEVCConfigurationBox(
            HEVCDecoderConfigurationRecord(
                configurationVersion = 1,
                generalProfileSpace = 0,
                generalTierFlag = false,
                generalProfileIdc = HEVCProfile.MAIN,
                generalProfileCompatibilityFlags = 0x60000000,
                generalConstraintIndicatorFlags = 0x800000000000,
                generalLevelIdc = 120,
                minSpatialSegmentationIdc = 0,
                parallelismType = 0,
                chromaFormat = ChromaFormat.YUV420,
                bitDepthLumaMinus8 = 0,
                bitDepthChromaMinus8 = 0,
                averageFrameRate = 0,
                constantFrameRate = 0,
                numTemporalLayers = 1,
                temporalIdNested = true,
                lengthSizeMinusOne = 3,
                parameterSets = listOf(
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.VPS,
                        vps
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.SPS,
                        sps
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.PPS,
                        pps
                    )
                )
            )
        )
        val buffer = hvcC.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}