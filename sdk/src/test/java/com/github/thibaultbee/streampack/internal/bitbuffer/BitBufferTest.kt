package com.github.thibaultbee.streampack.internal.bitbuffer

import com.github.thibaultbee.streampack.utils.Utils
import org.junit.Assert
import org.junit.Test

class BitBufferTest {
    @Test
    fun `put aligned ByteBuffer`() {
        val bitBuffer = BitBuffer.allocate(20)
        val randomByteBuffer = Utils.generateRandomBuffer(9)
        bitBuffer.put(0xFF.toByte())
        bitBuffer.put(randomByteBuffer)
        Assert.assertArrayEquals(
            bitBuffer.toByteBuffer().array().copyOfRange(1, randomByteBuffer.limit() + 1),
            randomByteBuffer.array()
        )
    }

    @Test
    fun `put aligned ByteArray`() {
        val bitBuffer = BitBuffer.allocate(20)
        val randomByteArray = Utils.generateRandomArray(9)
        bitBuffer.put(0xFF.toByte())
        bitBuffer.put(randomByteArray)
        Assert.assertArrayEquals(
            bitBuffer.toByteBuffer().array().copyOfRange(1, randomByteArray.size + 1),
            randomByteArray
        )
    }
}