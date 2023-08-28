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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackFragmentBaseMediaDecodeTimeBoxTest {
    @Test
    fun `write valid tfdt test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("tfdt.box")
        val tfdt = TrackFragmentBaseMediaDecodeTimeBox(
            baseMediaDecodeTime = 15360,
            version = 1
        )
        val buffer = tfdt.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}