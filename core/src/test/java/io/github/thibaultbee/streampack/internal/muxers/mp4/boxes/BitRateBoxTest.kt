package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BitRateBoxTest {
    @Test
    fun `write valid btrt test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("btrt.box")
        val btrt = BitRateBox(1100000, 4840000, 3878679)
        val buffer = btrt.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}