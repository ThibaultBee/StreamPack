package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MovieHeaderBoxTest {
    @Test
    fun `write valid mvhd test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("mvhd.box")
        val mvhd = MovieHeaderBox(
            version = 0.toByte(),
            creationTime = 0,
            modificationTime = 0,
            timescale = 1000,
            rate = 1.0f,
            volume = 1.0f,
            duration = 45000,
            nextTrackId = 2
        )
        val buffer = mvhd.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}