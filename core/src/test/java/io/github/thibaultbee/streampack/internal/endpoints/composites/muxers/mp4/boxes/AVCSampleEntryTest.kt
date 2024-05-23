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
import io.github.thibaultbee.streampack.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.utils.MockUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class AVCSampleEntryTest {
    @Test
    fun `write valid avc1 test`() {
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x67,
                0x64,
                0x00,
                0x1F,
                0xAC.toByte(),
                0xD9.toByte(),
                0xC0.toByte(),
                0x44,
                0x03,
                0xC7.toByte(),
                0x88.toByte(),
                0xE1.toByte(),
                0x00,
                0x00,
                0x03,
                0x00,
                0x01,
                0x00,
                0x00,
                0x03,
                0x00,
                0x3C,
                0x0F,
                0x18,
                0x31,
                0x9E.toByte()
            )
        )
        val pps =
            ByteBuffer.wrap(byteArrayOf(0x68, 0xE9.toByte(), 0xBB.toByte(), 0x2C, 0x8B.toByte()))
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("avc1.box")
        val avcC = AVCConfigurationBox(
            AVCDecoderConfigurationRecord(
                profileIdc = 100,
                profileCompatibility = 0,
                levelIdc = 31,
                sps = listOf(sps),
                pps = listOf(pps)
            )
        )
        val btrt = BitRateBox(1100000, 4840000, 3878679)

        val avc1 =
            AVCSampleEntry(
                MockUtils.mockSize(1074, 1920),
                compressorName = null,
                avcC = avcC,
                btrt = btrt
            )
        val buffer = avc1.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}