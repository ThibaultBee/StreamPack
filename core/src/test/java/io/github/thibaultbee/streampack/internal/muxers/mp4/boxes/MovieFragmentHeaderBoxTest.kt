package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MovieFragmentHeaderBoxTest {
    @Test
    fun `write valid mfhd test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("mfhd.box")
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = 2
        )
        val buffer = mfhd.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}