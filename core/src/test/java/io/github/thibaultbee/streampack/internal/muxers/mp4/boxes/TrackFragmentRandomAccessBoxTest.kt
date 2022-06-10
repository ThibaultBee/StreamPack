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

import io.github.thibaultbee.streampack.internal.utils.extensions.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert
import org.junit.Test

class TrackFragmentRandomAccessBoxTest {
    @Test
    fun `write valid tfra test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("tfra.box")
        val tfra = TrackFragmentRandomAccessBox(
            id = 1,
            listOf(
                TrackFragmentRandomAccessBox.Entry(
                    time = 15717,
                    moofOffset = 82533
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 31077,
                    moofOffset = 162702
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 46437,
                    moofOffset = 243560
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 61797,
                    moofOffset = 324780
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 77157,
                    moofOffset = 405267
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 92517,
                    moofOffset = 486192
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 107877,
                    moofOffset = 567498
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 123237,
                    moofOffset = 648058
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 138597,
                    moofOffset = 729525
                ),
            )
        )
        val buffer = tfra.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}