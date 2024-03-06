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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SampleToChunkBoxTest {
    @Test
    fun `write valid stsc test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("stsc.box")
        val stsc = SampleToChunkBox(
            listOf(
                SampleToChunkBox.Entry(1, 435, 1),
                SampleToChunkBox.Entry(3, 432, 1),
                SampleToChunkBox.Entry(4, 48, 1)
            )
        )
        val buffer = stsc.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}