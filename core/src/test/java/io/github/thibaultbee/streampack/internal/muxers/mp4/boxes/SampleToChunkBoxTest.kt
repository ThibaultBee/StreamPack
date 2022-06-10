package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SampleToChunkBoxTest {
    @Test
    fun `write valid stsc test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("stsc.box")
        val stsc = SampleToChunkBox(
            listOf(
                SampleToChunkBox.Entry(1, 435, 1),
                SampleToChunkBox.Entry(3, 432, 1),
                SampleToChunkBox.Entry(4, 48, 1)
            )
        )
        val buffer = stsc.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}