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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.opus

import io.github.thibaultbee.streampack.core.internal.utils.ResourcesUtils
import org.junit.Assert
import org.junit.Test

class OpusCsdParserTest {
    @Test
    fun `parse csd syntax`() {
        val expectedBuffer = ResourcesUtils.readByteBuffer(TEST_SAMPLES_DIR + "opus.csd")

        val triple = OpusCsdParser.parse(expectedBuffer)

        Assert.assertEquals(1.toByte(), triple.first.version)
        Assert.assertEquals(2.toByte(), triple.first.channelCount)
        Assert.assertEquals(312.toShort(), triple.first.preSkip)
        Assert.assertEquals(48000, triple.first.inputSampleRate)
        Assert.assertEquals(0.toShort(), triple.first.outputGain)
        Assert.assertEquals(0.toByte(), triple.first.channelMappingFamily)
        Assert.assertNull(triple.first.channelMapping)
    }

    companion object {
        const val TEST_SAMPLES_DIR = "test-samples/utils/av/audio/opus/"
    }
}