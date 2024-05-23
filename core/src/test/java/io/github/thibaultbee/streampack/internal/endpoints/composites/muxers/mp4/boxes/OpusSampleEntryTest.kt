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
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert
import org.junit.Test

class OpusSampleEntryTest {
    @Test
    fun `write valid Opus test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("Opus.box")

        val btrt = BitRateBox(0, 95230, 95230)
        val dOps = OpusSpecificBox(
            outputChannelCount = 1,
            preSkip = 312,
            inputSampleRate = 48000,
            outputGain = 0,
            channelMappingFamily = 0
        )

        val opus = OpusSampleEntry(
            channelCount = 1,
            dOps = dOps,
            btrt = btrt
        )
        val buffer = opus.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}