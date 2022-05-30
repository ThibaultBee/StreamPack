package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VideoMediaHeaderBoxTest {
    @Test
    fun `write valid vmhd test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("vmhd.box")
        val vmhd = VideoMediaHeaderBox()
        val buffer = vmhd.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}