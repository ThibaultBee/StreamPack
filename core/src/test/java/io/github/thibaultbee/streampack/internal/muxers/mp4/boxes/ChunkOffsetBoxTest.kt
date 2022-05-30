package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ChunkOffsetBoxTest {
    @Test
    fun `write valid stco test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("stco.box")
        val stco = ChunkOffsetBox(
            listOf(
                48, 1048191, 2070322, 3117965
            )
        )
        val buffer = stco.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}