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

class SyncSampleBoxTest {
    @Test
    fun `write valid stss test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("stss.box")
        val stss = SyncSampleBox(
            listOf(
                1,
                31,
                61,
                91,
                121,
                151,
                181,
                211,
                241,
                271,
                301,
                331,
                361,
                391,
                421,
                451,
                481,
                511,
                541,
                571,
                601,
                631,
                661,
                691,
                721,
                751,
                781,
                811,
                841,
                871,
                901,
                931,
                961,
                991,
                1021,
                1051,
                1081,
                1111,
                1141,
                1171,
                1201,
                1231,
                1261,
                1291,
                1321
            )
        )
        val buffer = stss.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }
}