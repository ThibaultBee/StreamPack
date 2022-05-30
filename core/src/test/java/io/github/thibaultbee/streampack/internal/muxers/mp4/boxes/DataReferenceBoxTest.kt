package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class DataReferenceBoxTest {
    @Test
    fun `write valid dref test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("dref.box")
        val dref = DataReferenceBox(listOf(DataEntryUrlBox()))
        val buffer = dref.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }

    @Test
    fun `empty entry list test`() {
        try {
            DataReferenceBox(listOf())
            fail("Should have thrown an exception")
        } catch (e: Exception) {
        }
    }
}