package com.github.thibaultbee.srtstreamer.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class BitSetExtensionsKtTest {
    @Test
    fun setIntTest() {
        val bitSet = BitSet(32)
        bitSet.set(3, 5, 0xF)
        assertTrue(bitSet[3])
        assertTrue(bitSet[4])
        assertTrue(bitSet[5])
    }

    @Test
    fun setByteTest() {
        val bitSet = BitSet(32)
        bitSet.set(4, 8, 0xFFFF.toByte())
        assertTrue(bitSet[4])
        assertTrue(bitSet[5])
        assertTrue(bitSet[6])
        assertTrue(bitSet[7])
        assertTrue(bitSet[8])
    }

    @Test
    fun toByteBufferTest() {
        val bitSet = BitSet(32)
        bitSet.set(4, 8, 0xFF.toByte())
        val buffer = bitSet.toByteBuffer()
        assertEquals(0xF0.toByte(), buffer.get(0))
        assertEquals(1.toByte(), buffer.get(1))
        try {
            buffer.get(2)
            fail()
        } catch (e: Exception) {
        }
    }
}