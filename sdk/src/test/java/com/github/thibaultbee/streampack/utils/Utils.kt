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