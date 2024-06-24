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
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SoundMediaHeaderBoxTest {
    @Test
    fun `write valid smhd test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("smhd.box")
        val smhd = SoundMediaHeaderBox()
        val buffer = smhd.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}