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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.core.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class AVCConfigurationBoxTest {
    @Test
    fun `write valid avcC test`() {
        val sps = ByteBuffer.wrap(
            byteArrayOf(
                0x67,
                0x42,
                0xC0.toByte(),
                0x32,
                0xDA.toByte(),
                0x00,
                0x80.toByte(),
                0x02,
                0x46,
                0xC0.toByte(),
                0x44,
                0x00,
                0x00,
                0x03,
                0x00,
                0x04,
                0x00,
                0x00,
                0x03,
                0x00,
                0xF2.toByte(),
                0x3C,
                0x60,
                0xCA.toByte(),
                0x80.toByte()
            )
        )
        val pps = ByteBuffer.wrap(byteArrayOf(0x68, 0xCE.toByte(), 0x09, 0xC8.toByte()))
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("avcC.box")
        val avcC = AVCConfigurationBox(
            AVCDecoderConfigurationRecord(
                profileIdc = 66,
                profileCompatibility = 192.toByte(),
                levelIdc = 50,
                sps = listOf(sps),
                pps = listOf(pps)
            )
        )
        val buffer = avcC.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}