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

import io.github.thibaultbee.streampack.internal.muxers.mp4.MP4ResourcesUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class DataReferenceBoxTest {
    @Test
    fun `write valid dref test`() {
        val expectedBuffer = MP4ResourcesUtils.readByteBuffer("dref.box")
        val dref = DataReferenceBox(listOf(DataEntryUrlBox()))
        val buffer = dref.toByteBuffer()
        assertArrayEquals(expectedBuffer.toByteArray(), buffer.toByteArray())
    }

    @Test
    fun `empty entry list test`() {
        try {
            DataReferenceBox(listOf())
            fail("Should have thrown an exception")
        } catch (_: Exception) {
        }
    }
}