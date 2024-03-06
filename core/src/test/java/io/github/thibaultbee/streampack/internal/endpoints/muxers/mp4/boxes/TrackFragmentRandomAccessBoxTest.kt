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
import org.junit.Assert
import org.junit.Test

class TrackFragmentRandomAccessBoxTest {
    @Test
    fun `write valid tfra test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("tfra.box")
        val tfra = TrackFragmentRandomAccessBox(
            id = 1,
            listOf(
                TrackFragmentRandomAccessBox.Entry(
                    time = 15717L,
                    moofOffset = 82533L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 31077L,
                    moofOffset = 162702L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 46437L,
                    moofOffset = 243560L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 61797L,
                    moofOffset = 324780L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 77157L,
                    moofOffset = 405267L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 92517L,
                    moofOffset = 486192L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 107877L,
                    moofOffset = 567498L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 123237L,
                    moofOffset = 648058L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 138597L,
                    moofOffset = 729525L
                ),
            )
        )
        val buffer = tfra.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}