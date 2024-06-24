package io.github.thibaultbee.streampack.core.internal.utils.av

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class BitBufferTest {

    @Test
    fun `test out of range`() {
        val byteBuffer = ByteBuffer.allocate(2)
        val bitBuffer = BitBuffer(byteBuffer)
        try {
            bitBuffer.getLong(17)
            fail("Should throw exception")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
        }

        try {
            bitBuffer.put(111, 17)
            fail("Should throw exception")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
        }
    }

    @Test
    fun `test remaining bits`() {
        val byteBuffer = ByteBuffer.allocate(4)
        val bitBuffer = BitBuffer(byteBuffer)
        assertEquals(32, bitBuffer.bitRemaining)
        assertEquals(true, bitBuffer.hasRemaining)

        bitBuffer.getBoolean() // Go to next bit
        assertEquals(31, bitBuffer.bitRemaining)
        assertEquals(true, bitBuffer.hasRemaining)

        bitBuffer.getLong(7) // Go to next byte
        assertEquals(24, bitBuffer.bitRemaining)
        assertEquals(true, bitBuffer.hasRemaining)

        bitBuffer.getLong(24) // Go to the end
        assertEquals(0, bitBuffer.bitRemaining)
        assertEquals(false, bitBuffer.hasRemaining)
    }

    @Test
    fun `test read byte per byte`() {
        val array = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteBuffer = ByteBuffer.wrap(array)
        val bitBuffer = BitBuffer(byteBuffer)
        assertEquals(0x01, bitBuffer.getLong(8))
        assertEquals(0x02, bitBuffer.getLong(8))
        assertEquals(0x03, bitBuffer.getLong(8))
        assertEquals(0x04, bitBuffer.getLong(8))
    }

    @Test
    fun `test read non align byte`() {
        val array = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteBuffer = ByteBuffer.wrap(array)
        val bitBuffer = BitBuffer(byteBuffer)
        bitBuffer.getBoolean() // Go to next bit to be non align
        assertEquals(0x02, bitBuffer.getLong(8))
        assertEquals(0x04, bitBuffer.getLong(8))
        assertEquals(0x06, bitBuffer.getLong(8))
        assertEquals(0x04, bitBuffer.getLong(7))
    }

    @Test
    fun `test write byte per byte`() {
        val bitBuffer = BitBuffer(ByteBuffer.allocate(4))
        bitBuffer.put(0x01, 8)
        bitBuffer.put(0x02, 8)
        bitBuffer.put(0x03, 8)
        bitBuffer.put(0x04, 8)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), bitBuffer.buffer.array())
    }

    @Test
    fun `test write non align byte`() {
        val bitBuffer = BitBuffer(ByteBuffer.allocate(4))
        bitBuffer.put(0, 1)
        bitBuffer.put(0x02, 8)
        bitBuffer.put(0x04, 8)
        bitBuffer.put(0x06, 8)
        bitBuffer.put(0x04, 7)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), bitBuffer.buffer.array())
    }

    @Test
    fun `test write byte buffer`() {
        val byteBufferToRead = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))

        val bitBuffer = BitBuffer(ByteBuffer.allocate(4))
        bitBuffer.put(byteBufferToRead)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), bitBuffer.buffer.array())
    }

    @Test
    fun `test write aligned bit buffer`() {
        val byteBufferToRead = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val bitBufferToRead = BitBuffer(byteBufferToRead)

        val bitBuffer = BitBuffer(ByteBuffer.allocate(4))
        bitBuffer.put(bitBufferToRead)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), bitBuffer.buffer.array())
    }

    @Test
    fun `test write limited bit buffer`() {
        val byteBufferToRead = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x07))
        val bitBufferToRead = BitBuffer(byteBufferToRead, bitEnd = 29)

        val bitBuffer = BitBuffer(ByteBuffer.allocate(4))
        bitBuffer.put(bitBufferToRead)

        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), bitBuffer.buffer.array())
    }
}