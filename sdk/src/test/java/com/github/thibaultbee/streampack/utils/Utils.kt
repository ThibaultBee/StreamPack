/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.utils

import java.nio.ByteBuffer
import kotlin.random.Random

object Utils {
    /**
     * Generates a randomized direct ByteBuffer
     * @param size size of buffer to generates
     * @return random ByteBuffer
     */
    fun generateRandomDirectBuffer(size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(size)
        buffer.put(generateRandomArray(size))
        buffer.rewind()
        return buffer
    }

    /**
     * Generates a randomized ByteBuffer
     * @param size size of buffer to generates
     * @return random ByteBuffer
     */
    fun generateRandomBuffer(size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)
        buffer.put(generateRandomArray(size))
        buffer.rewind()
        return buffer
    }

    /**
     * Generates a randomized ByteArray
     * @param size size of buffer to generates
     * @return random ByteBuffer
     */
    fun generateRandomArray(size: Int): ByteArray {
        return Random.nextBytes(size)
    }
}