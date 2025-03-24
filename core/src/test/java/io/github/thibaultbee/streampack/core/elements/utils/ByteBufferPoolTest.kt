/*
 * Copyright 2025 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils

import io.github.thibaultbee.streampack.core.elements.utils.pool.ByteBufferPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.nio.ByteBuffer

class ByteBufferPoolTest {
    @Test
    fun `get should return a buffer`() {
        val pool = ByteBufferPool(true)
        val buffer = pool.get(10)
        assertEquals(10, buffer.capacity())
    }

    @Test
    fun `get should return a put buffer`() {
        val pool = ByteBufferPool(true)
        val buffer = ByteBuffer.allocate(10)
        pool.put(buffer)
        var newBuffer = pool.get(10)
        assertEquals(System.identityHashCode(buffer), System.identityHashCode(newBuffer))
        newBuffer = pool.get(10)
        assertNotEquals(System.identityHashCode(buffer), System.identityHashCode(newBuffer))
    }

    @Test
    fun `get should return a put larger buffer`() {
        val pool = ByteBufferPool(true)
        val buffer = ByteBuffer.allocate(10)
        pool.put(buffer)
        val newBuffer = pool.get(5)
        assertEquals(System.identityHashCode(buffer), System.identityHashCode(newBuffer))
    }
}