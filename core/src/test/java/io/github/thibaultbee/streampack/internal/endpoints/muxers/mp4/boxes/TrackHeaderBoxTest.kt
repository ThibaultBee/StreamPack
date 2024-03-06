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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.utils.MockUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackHeaderBoxTest {
    @Test
    fun `write valid tkhd test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("tkhd.box")
        val tkhd = TrackHeaderBox(
            version = 0.toByte(),
            flags = listOf(TrackHeaderBox.TrackFlag.ENABLED, TrackHeaderBox.TrackFlag.IN_MOVIE),
            creationTime = 0,
            modificationTime = 0,
            id = 1,
            layer = 0,
            alternateGroup = 0,
            volume = 0.0f,
            duration = 45000,
            resolution = MockUtils.mockSize(2048, 1152)
        )
        val buffer = tkhd.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}