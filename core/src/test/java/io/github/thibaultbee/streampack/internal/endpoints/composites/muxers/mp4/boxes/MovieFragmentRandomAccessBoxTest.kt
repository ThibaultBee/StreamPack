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

class MovieFragmentRandomAccessBoxTest {
    @Test
    fun `write valid tfra test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("mfra.box")

        val tfra1 = TrackFragmentRandomAccessBox(
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

        val tfra2 = TrackFragmentRandomAccessBox(
            id = 2,
            listOf(
                TrackFragmentRandomAccessBox.Entry(
                    time = 46080L,
                    moofOffset = 82533L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 90112L,
                    moofOffset = 162702L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 134144L,
                    moofOffset = 243560L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 178176L,
                    moofOffset = 324780L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 222208L,
                    moofOffset = 405267L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 266240L,
                    moofOffset = 486192L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 310272L,
                    moofOffset = 567498L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 354304L,
                    moofOffset = 648058L
                ),
                TrackFragmentRandomAccessBox.Entry(
                    time = 398336L,
                    moofOffset = 729525L
                ),
            )
        )

        val mfra = MovieFragmentRandomAccessBox(listOf(tfra1, tfra2))
        val buffer = mfra.toByteBuffer()
        Assert.assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}