package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrackFragmentBaseMediaDecodeTimeBoxTest {
    @Test
    fun `write valid tfdt test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("tfdt.box")
        val tfdt = TrackFragmentBaseMediaDecodeTimeBox(
            baseMediaDecodeTime = 15360,
            version = 1
        )
        val buffer = tfdt.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}