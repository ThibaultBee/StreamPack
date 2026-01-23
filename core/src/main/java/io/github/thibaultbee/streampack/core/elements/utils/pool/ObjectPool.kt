/*
 * Copyright 2026 Thibault B.
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
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A pool of objects.
 *
 * The pool is thread-safe.
 *
 * The implementation is required to add a `get` methods.
 *
 * @param T the type of object to pool
 */
internal sealed class ObjectPool<T>() : Closeable {
    private val pool = ArrayDeque<T>()

    private val isClosed = AtomicBoolean(false)

    protected fun get(): T? {
        if (isClosed.get()) {
            throw IllegalStateException("ObjectPool is closed")
        }

        return synchronized(pool) {
            if (!pool.isEmpty()) {
                pool.removeFirst()
            } else {
                null
            }
        }
    }

    /**
     * Puts an object in the pool.
     *
     * @param any the object to put
     */
    fun put(any: T) {
        if (isClosed.get()) {
            throw IllegalStateException("ObjectPool is closed")
        }
        synchronized(pool) {
            pool.addLast(any)
        }
    }

    /**
     * Clears the pool.
     */
    fun clear() {
        if (isClosed.get()) {
            return
        }
        synchronized(pool) {
            pool.clear()
        }
    }

    /**
     * Closes the pool.
     *
     * After a pool is closed, it cannot be used anymore.
     */
    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
        synchronized(pool) {
            pool.clear()
        }
    }
}