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

class TrackFragmentHeaderBoxTest {
    @Test
    fun `write valid tfhd test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("tfhd.box")
        val tfhd = TrackFragmentHeaderBox(
            id = 1,
            baseDataOffset = 72077L,
            defaultSampleDuration = 512,
            defaultSampleSize = 28381,
            defaultSampleFlags = SampleFlags(
                dependsOn = SampleDependsOn.OTHERS,
                isNonSyncSample = true
            ),
            durationIsEmpty = false

        )
        val buffer = tfhd.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}