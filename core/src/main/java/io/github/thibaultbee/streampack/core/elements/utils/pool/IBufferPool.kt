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

import java.nio.Buffer

interface IGetOnlyBufferPool<T : Buffer> {
    /**
     * Get a buffer from the pool.
     *
     * @param capacity buffer capacity
     * @return buffer
     */
    fun get(capacity: Int): T
}

/**
 * Interface to manage buffer pool.
 *
 * @param T type of buffer to manage
 */
interface IBufferPool<T : Buffer> : IGetOnlyBufferPool<T> {
    /**
     * Put a buffer in the pool.
     *
     * @param buffer buffer to put
     */
    fun put(buffer: T)

    /**
     * Clear the pool.
     */
    fun clear()

    /**
     * Close the pool.
     *
     * After a pool is closed, it cannot be used anymore.
     */
    fun close()
}