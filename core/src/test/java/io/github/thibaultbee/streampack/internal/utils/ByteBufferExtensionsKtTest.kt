package io.github.thibaultbee.streampack.internal.utils

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class ByteBufferExtensionsKtTest {
    @Test
    fun `slices with multiple prefix`() {
        val testBuffer =
            ByteBuffer.wrap(
                byteArrayOf(
                    0,
                    0,
                    0,
                    1,
                    24,
                    53,
                    2,
                    0,
                    0,
                    0,
                    1,
                    34,
                    45,
                    98,
                    0,
                    0,
                    0,
                    1,
                    3,
                    56,
                    2
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
        val testBuffer =
            ByteBuffer.wrap(
                byteArrayOf(
                    2,
                    0,
                    1,
                    24,
                    3,
                    53
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
        val testBuffer =
            ByteBuffer.wrap(
                byteArrayOf(
                    0,
                    0,
                    0,
                    1,
                    24,
                    53
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
        val testBuffer =
            ByteBuffer.wrap(
                byteArrayOf(
                    2,
                    3,
                    4,
                    0,
                    0,
                    0,
                    1,
                    24,
                    53
                )
            )

        val resultBuffers = testBuffer.slices(
            byteArrayOf(0, 0, 0, 1)
        )
        assertEquals(
            1, resultBuffers.size
        )
        assertArrayEquals(
            testBuffer.array()
                .sliceArray(IntRange(3, 8)),
            resultBuffers[0].array()
        )
    }
}