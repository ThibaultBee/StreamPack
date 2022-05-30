package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class DataInformationBoxTest {
    @Test
    fun `write valid dinf test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("dinf.box")
        val dinf = DataInformationBox(DataReferenceBox(listOf(DataEntryUrlBox())))
        val buffer = dinf.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}