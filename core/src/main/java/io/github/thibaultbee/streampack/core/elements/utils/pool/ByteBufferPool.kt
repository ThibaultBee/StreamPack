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
package io.github.thibaultbee.streampack.core.elements.utils.pool

import java.io.Closeable
import java.nio.ByteBuffer
import java.util.NavigableMap
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A pool of ByteBuffers.
 *
 * Based on an idea from [Pbbl](https://github.com/jhg023/Pbbl)
 */
class ByteBufferPool(private val isDirect: Boolean) : IBufferPool<ByteBuffer>, Closeable {
    private val buffers: NavigableMap<Int, ArrayDeque<ByteBuffer>> = TreeMap()

    private val isClosed = AtomicBoolean(false)

    private fun allocate(capacity: Int) = if (isDirect) {
        ByteBuffer.allocateDirect(capacity)
    } else {
        ByteBuffer.allocate(capacity)
    }

    override fun get(capacity: Int): ByteBuffer {
        if (isClosed.get()) {
            throw IllegalStateException("ByteBufferPool is closed")
        }

        val buffer = synchronized(buffers) {
            buffers.tailMap(
                capacity,
                true
            ).values
                .mapNotNull(ArrayDeque<ByteBuffer>::removeFirstOrNull)
                .firstOrNull()
        }
        return if (buffer != null) {
            buffer.clear().limit(capacity)
            buffer
        } else {
            allocate(capacity)
        }
    }

    override fun put(buffer: ByteBuffer) {
        if (isClosed.get()) {
            throw IllegalStateException("ByteBufferPool is closed")
        }
        synchronized(buffers) {
            val queue = buffers[buffer.capacity()]
            if (queue != null) {
                queue.add(buffer)
            } else {
                buffers[buffer.capacity()] = ArrayDeque<ByteBuffer>(3).apply { addLast(buffer) }
            }
        }
    }

    override fun clear() {
        if (isClosed.get()) {
            return
        }
        synchronized(buffers) {
            buffers.clear()
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
        synchronized(buffers) {
            buffers.clear()
        }
    }
}