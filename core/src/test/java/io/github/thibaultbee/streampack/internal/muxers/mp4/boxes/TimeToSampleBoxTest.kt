package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.muxers.mp4.models.DecodingTime
import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TimeToSampleBoxTest {
    @Test
    fun `write valid stts test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("stts.box")
        val stts = TimeToSampleBox(
            listOf(DecodingTime(1350, 512))
        )
        val buffer = stts.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}