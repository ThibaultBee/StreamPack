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
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.SampleDependsOn
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackRunBoxTest {
    @Test
    fun `write valid trun test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("trun.box")
        val entries = listOf(
            TrackRunBox.Entry(sampleSize = 28381),
            TrackRunBox.Entry(sampleSize = 889),
            TrackRunBox.Entry(sampleSize = 1383),
            TrackRunBox.Entry(sampleSize = 1276),
            TrackRunBox.Entry(sampleSize = 1167),
            TrackRunBox.Entry(sampleSize = 1367),
            TrackRunBox.Entry(sampleSize = 1570),
            TrackRunBox.Entry(sampleSize = 1206),
            TrackRunBox.Entry(sampleSize = 2088),
            TrackRunBox.Entry(sampleSize = 1466),
            TrackRunBox.Entry(sampleSize = 1741),
            TrackRunBox.Entry(sampleSize = 1069),
            TrackRunBox.Entry(sampleSize = 1451),
            TrackRunBox.Entry(sampleSize = 1393),
            TrackRunBox.Entry(sampleSize = 1196),
            TrackRunBox.Entry(sampleSize = 1363),
            TrackRunBox.Entry(sampleSize = 1667),
            TrackRunBox.Entry(sampleSize = 1247),
            TrackRunBox.Entry(sampleSize = 2091),
            TrackRunBox.Entry(sampleSize = 1475),
            TrackRunBox.Entry(sampleSize = 2164),
            TrackRunBox.Entry(sampleSize = 1043),
            TrackRunBox.Entry(sampleSize = 1324),
            TrackRunBox.Entry(sampleSize = 1402),
            TrackRunBox.Entry(sampleSize = 1206),
            TrackRunBox.Entry(sampleSize = 1366),
            TrackRunBox.Entry(sampleSize = 1671),
            TrackRunBox.Entry(sampleSize = 1638),
            TrackRunBox.Entry(sampleSize = 2223),
            TrackRunBox.Entry(sampleSize = 1548)
        )
        val trun = TrackRunBox(
            version = 0,
            dataOffset = 240,
            firstSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.NO_OTHER,
                isNonSyncSample = false
            ),
            entries = entries
        )
        val buffer = trun.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}