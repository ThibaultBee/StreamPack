package io.github.thibaultbee.streampack.core.internal.utils.extensions

import io.github.thibaultbee.streampack.core.internal.utils.Utils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class ByteBufferExtensionsKtTest {
    @Test
    fun `byteArray raw`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.wrap(
            byteArray
        )

        assertArrayEquals(
            byteArray, testBuffer.toByteArray()
        )
    }

    @Test
    fun `byteArray direct raw`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.allocateDirect(
            byteArray.size
        )
        testBuffer.put(
            byteArray
        )
        testBuffer.rewind()

        assertArrayEquals(
            byteArray, testBuffer.toByteArray()
        )
    }

    @Test
    fun `byteArray with position != 0`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.wrap(
            byteArray
        )
        testBuffer.position(2)

        assertArrayEquals(
            byteArray.copyOfRange(2, 5), testBuffer.toByteArray()
        )
    }

    @Test
    fun `byteArray direct with position != 0`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.allocateDirect(
            byteArray.size
        )
        testBuffer.put(
            byteArray
        )
        testBuffer.position(2)

        assertArrayEquals(
            byteArray.copyOfRange(2, 5), testBuffer.toByteArray()
        )
    }

    @Test
    fun `byteArray with arrayOffset`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.wrap(
            byteArray
        )
        testBuffer.position(2)
        val slice = testBuffer.slice()

        assertArrayEquals(
            byteArray.copyOfRange(2, 5), slice.toByteArray()
        )
    }

    @Test
    fun `byteArray direct with arrayOffset`() {
        val byteArray = byteArrayOf(
            1, 2, 3, 4, 5
        )
        val testBuffer = ByteBuffer.allocateDirect(
            byteArray.size
        )
        testBuffer.put(
            byteArray
        )
        testBuffer.position(2)
        val slice = testBuffer.slice()

        assertArrayEquals(
            byteArray.copyOfRange(2, 5), slice.toByteArray()
        )
    }

    @Test
    fun `clone with position`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                1, 2, 3, 4, 5
            )
        )
        testBuffer.position(2)

        val clonedBuffer = testBuffer.clone()
        assertArrayEquals(
            testBuffer.toByteArray(), clonedBuffer.toByteArray()
        )
    }

    @Test
    fun `slices with multiple prefix`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                0, 0, 0, 1, 24, 53, 2, 0, 0, 0, 1, 34, 45, 98, 0, 0, 0, 1, 3, 56, 2
            )
        )

        val resultBuffers = testBuffer.slices(
            byteArrayOf(0, 0, 0, 1)
        )
        assertEquals(
            3, resultBuffers.size
        )
        var resultArray = ByteArray(0)
        resultBuffers.forEach { resultArray += it.array() } // concat all arrays
        assertArrayEquals(testBuffer.array(), resultArray)
    }

    @Test
    fun `slices without prefix`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                2, 0, 1, 24, 3, 53
            )
        )

        val resultBuffers = testBuffer.slices(
            byteArrayOf(0, 0, 0, 1)
        )
        assertEquals(
            0, resultBuffers.size
        )
    }

    @Test
    fun `slices with single prefix at start`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                0, 0, 0, 1, 24, 53
            )
        )

        val resultBuffers = testBuffer.slices(
            byteArrayOf(0, 0, 0, 1)
        )
        assertEquals(
            1, resultBuffers.size
        )
        assertArrayEquals(testBuffer.array(), resultBuffers[0].array())
    }

    @Test
    fun `slices with single prefix`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                2, 3, 4, 0, 0, 0, 1, 24, 53
            )
        )

        val resultBuffers = testBuffer.slices(
            byteArrayOf(0, 0, 0, 1)
        )
        assertEquals(
            1, resultBuffers.size
        )
        assertArrayEquals(
            testBuffer.array().sliceArray(IntRange(3, 8)), resultBuffers[0].array()
        )
    }

    @Test
    fun `extractRbsp with single emulation prevention byte`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                66,
                0,
                0,
                3,
                1
            )
        )
        val expectedArray = byteArrayOf(
            66,
            0,
            0,
            1
        )
        val resultBuffer = testBuffer.extractRbsp(0)

        assertArrayEquals(
            expectedArray, resultBuffer.toByteArray()
        )
    }

    @Test
    fun `extractRbsp with start code and with multiple emulation prevention bytes`() {
        val testBuffer = ByteBuffer.wrap(
            byteArrayOf(
                0,
                0,
                0,
                1,
                66,
                1,
                1,
                1,
                96,
                0,
                0,
                3,
                0,
                -80,
                0,
                0,
                3,
                0,
                0,
                3,
                0,
                60,
                -96,
                8,
                8,
                5,
                7,
                19,
                -27,
                -82,
                -28,
                -55,
                46,
                -96,
                11,
                -76,
                40,
                74
            )
        )
        val expectedArray = byteArrayOf(
            66,
            1,
            1,
            1,
            96,
            0,
            0,
            0,
            -80,
            0,
            0,
            0,
            0,
            0,
            60,
            -96,
            8,
            8,
            5,
            7,
            19,
            -27,
            -82,
            -28,
            -55,
            46,
            -96,
            11,
            -76,
            40,
            74
        )
        val resultBuffer = testBuffer.extractRbsp(2)

        assertArrayEquals(
            expectedArray, resultBuffer.toByteArray()
        )
    }

    @Test
    fun `putString test`() {
        val value = "Hello World"
        val testBuffer = ByteBuffer.allocate(value.length)
        testBuffer.putString(value)
        testBuffer.rewind()
        assertArrayEquals(
            value.toByteArray(),
            testBuffer.toByteArray()
        )
    }

    @Test
    fun `getString test`() {
        val value = "AOPUS"
        val testBuffer = ByteBuffer.wrap("AOPUS".toByteArray())
        assertEquals(testBuffer.getString(5), value)

        val testBuffer2 = Utils.generateRandomDirectBuffer(10)
        testBuffer2.putString(value)
        testBuffer2.rewind()
        assertEquals(testBuffer2.getString(5), value)
    }
}