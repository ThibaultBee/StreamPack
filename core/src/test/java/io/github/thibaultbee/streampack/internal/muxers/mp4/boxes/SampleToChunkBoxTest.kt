package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.muxers.mp4.models.Chunk
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleToChunk
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
                SampleToChunk(1, 435, 1),
                SampleToChunk(3, 432, 1),
                SampleToChunk(4, 48, 1)
            )
        )
        val buffer = stsc.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}