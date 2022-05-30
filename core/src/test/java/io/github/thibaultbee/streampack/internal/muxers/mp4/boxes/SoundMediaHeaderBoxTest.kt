package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.*
import org.junit.Test

class SoundMediaHeaderBoxTest {
    @Test
    fun `write valid smhd test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("smhd.box")
        val smhd = SoundMediaHeaderBox()
        val buffer = smhd.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}