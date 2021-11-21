package com.github.thibaultbee.streampack.internal.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

class ByteBufferExtensionsKtTest {
    @Test
    fun `ByteBuffer getMaxValuePerChannel mono`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getMaxValuePerChannel(1).size)
        assertEquals(125.toByte(), buffer.getMaxValuePerChannel(1)[0])
    }

    @Test
    fun `ByteBuffer getMaxValuePerChannel stereo`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getMaxValuePerChannel(2).size)
        assertEquals(125.toByte(), buffer.getMaxValuePerChannel(2)[0])
        assertEquals(110.toByte(), buffer.getMaxValuePerChannel(2)[1])
    }

    @Test
    fun `ShortBuffer getMaxValuePerChannel mono`() {
        val buffer = ShortBuffer.wrap(shortArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getMaxValuePerChannel(1).size)
        assertEquals(125.toShort(), buffer.getMaxValuePerChannel(1)[0])
    }

    @Test
    fun `ShortBuffer getMaxValuePerChannel stereo`() {
        val buffer = ShortBuffer.wrap(shortArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getMaxValuePerChannel(2).size)
        assertEquals(125.toShort(), buffer.getMaxValuePerChannel(2)[0])
        assertEquals(110.toShort(), buffer.getMaxValuePerChannel(2)[1])
    }

    @Test
    fun `IntBuffer getMaxValuePerChannel mono`() {
        val buffer = IntBuffer.wrap(intArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getMaxValuePerChannel(1).size)
        assertEquals(125, buffer.getMaxValuePerChannel(1)[0])
    }

    @Test
    fun `IntBuffer getMaxValuePerChannel stereo`() {
        val buffer = IntBuffer.wrap(intArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getMaxValuePerChannel(2).size)
        assertEquals(125, buffer.getMaxValuePerChannel(2)[0])
        assertEquals(110, buffer.getMaxValuePerChannel(2)[1])
    }

    @Test
    fun `FloatBuffer getMaxValuePerChannel mono`() {
        val buffer = FloatBuffer.wrap(floatArrayOf(-125F, 0F, 20F, 110F))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getMaxValuePerChannel(1).size)
        assertEquals(125F, buffer.getMaxValuePerChannel(1)[0])
    }

    @Test
    fun `FloatBuffer getMaxValuePerChannel stereo`() {
        val buffer = FloatBuffer.wrap(floatArrayOf(-125F, 0F, 20F, 110F))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getMaxValuePerChannel(2).size)
        assertEquals(125F, buffer.getMaxValuePerChannel(2)[0])
        assertEquals(110F, buffer.getMaxValuePerChannel(2)[1])
    }


    @Test
    fun `ByteBuffer getSquareSumPerChannel mono`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getSquareSumPerChannel(1).size)
        assertEquals(28125F, buffer.getSquareSumPerChannel(1)[0])
    }

    @Test
    fun `ByteBuffer getSquareSumPerChannel stereo`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getSquareSumPerChannel(2).size)
        assertEquals(16025F, buffer.getSquareSumPerChannel(2)[0])
        assertEquals(12100F, buffer.getSquareSumPerChannel(2)[1])
    }

    @Test
    fun `ShortBuffer getSquareSumPerChannel mono`() {
        val buffer = ShortBuffer.wrap(shortArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getSquareSumPerChannel(1).size)
        assertEquals(28125F, buffer.getSquareSumPerChannel(1)[0])
    }

    @Test
    fun `ShortBuffer getSquareSumPerChannel stereo`() {
        val buffer = ShortBuffer.wrap(shortArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getSquareSumPerChannel(2).size)
        assertEquals(16025F, buffer.getSquareSumPerChannel(2)[0])
        assertEquals(12100F, buffer.getSquareSumPerChannel(2)[1])
    }

    @Test
    fun `IntBuffer getSquareSumPerChannel mono`() {
        val buffer = IntBuffer.wrap(intArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getSquareSumPerChannel(1).size)
        assertEquals(28125F, buffer.getSquareSumPerChannel(1)[0])
    }

    @Test
    fun `IntBuffer getSquareSumPerChannel stereo`() {
        val buffer = IntBuffer.wrap(intArrayOf(-125, 0, 20, 110))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getSquareSumPerChannel(2).size)
        assertEquals(16025F, buffer.getSquareSumPerChannel(2)[0])
        assertEquals(12100F, buffer.getSquareSumPerChannel(2)[1])
    }

    @Test
    fun `FloatBuffer getSquareSumPerChannel mono`() {
        val buffer = FloatBuffer.wrap(floatArrayOf(-125F, 0F, 20F, 110F))
        assertEquals(0, buffer.position())
        assertEquals(1, buffer.getSquareSumPerChannel(1).size)
        assertEquals(28125F, buffer.getSquareSumPerChannel(1)[0])
    }

    @Test
    fun `FloatBuffer getSquareSumPerChannel stereo`() {
        val buffer = FloatBuffer.wrap(floatArrayOf(-125F, 0F, 20F, 110F))
        assertEquals(0, buffer.position())
        assertEquals(2, buffer.getSquareSumPerChannel(2).size)
        assertEquals(16025F, buffer.getSquareSumPerChannel(2)[0])
        assertEquals(12100F, buffer.getSquareSumPerChannel(2)[1])
    }
}