package io.github.thibaultbee.streampack.core.internal.utils.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ListExtensionsKtTest {
    @Test
    fun unzip() {
        val list = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )
        val expected = listOf(
            listOf(1, 4),
            listOf(2, 5),
            listOf(3, 6)
        )
        assertEquals(expected, list.unzip())
    }

    @Test
    fun unzipWithDifferentSize() {
        val list = listOf(
            listOf(1, 2, 3),
            listOf(4, 5)
        )
        try {
            list.unzip()
            fail("Should throw IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Success
        }
    }

    @Test
    fun unzipWithDifferentSize2() {
        val list = listOf(
            listOf(1, 2),
            listOf(3, 4, 5)
        )
        try {
            list.unzip()
            fail("Should throw IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Success
        }
    }
}