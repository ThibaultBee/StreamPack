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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags

import android.media.MediaCodecInfo
import android.util.Size
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.core.internal.utils.MockUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class OnMetadataTest {
    @Test
    fun videoOnlyTest() {
        val expectedArray = byteArrayOf(
            0x12,
            0x0,
            0x0,
            0x8C.toByte(),
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x2,
            0x0,
            0xA,
            0x6F,
            0x6E,
            0x4D,
            0x65,
            0x74,
            0x61,
            0x44,
            0x61,
            0x74,
            0x61,
            0x8,
            0x0,
            0x0,
            0x0,
            0x6,
            0x0,
            0x8,
            0x64,
            0x75,
            0x72,
            0x61,
            0x74,
            0x69,
            0x6F,
            0x6E,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0xC,
            0x76,
            0x69,
            0x64,
            0x65,
            0x6F,
            0x63,
            0x6F,
            0x64,
            0x65,
            0x63,
            0x69,
            0x64,
            0x0,
            0x40,
            0x1C,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0xD,
            0x76,
            0x69,
            0x64,
            0x65,
            0x6F,
            0x64,
            0x61,
            0x74,
            0x61,
            0x72,
            0x61,
            0x74,
            0x65,
            0x0,
            0x40,
            0x9F.toByte(),
            0x40,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x5,
            0x77,
            0x69,
            0x64,
            0x74,
            0x68,
            0x0,
            0x40,
            0x84.toByte(),
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x6,
            0x68,
            0x65,
            0x69,
            0x67,
            0x68,
            0x74,
            0x0,
            0x40,
            0x7E,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x9,
            0x66,
            0x72,
            0x61,
            0x6D,
            0x65,
            0x72,
            0x61,
            0x74,
            0x65,
            0x0,
            0x40,
            0x3E,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x0,
            0x9,
            0x0,
            0x0,
            0x0,
            0x97.toByte()
        )

        MockUtils.mockSizeConstructor(640, 480)
        val onMetadata = OnMetadata.fromConfigs(
            listOf(
                VideoConfig(
                    resolution = Size(640, 480),
                    profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                    level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
                )
            )
        )
        val buffer = onMetadata.write()
        val resultArray = buffer.toByteArray()
        assertArrayEquals(expectedArray, resultArray)
    }
}