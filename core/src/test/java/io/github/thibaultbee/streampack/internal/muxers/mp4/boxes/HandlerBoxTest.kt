package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.*
import org.junit.Test

class HandlerBoxTest {
    @Test
    fun `write valid hdlr test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("hdlr.box")
        val hdlr = HandlerBox(
            type = HandlerBox.HandlerType.VIDEO,
            name = "VideoHandler"
        )
        val buffer = hdlr.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}