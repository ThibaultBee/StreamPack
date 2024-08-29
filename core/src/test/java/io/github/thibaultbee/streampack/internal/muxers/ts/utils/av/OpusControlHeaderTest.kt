package io.github.thibaultbee.streampack.internal.muxers.ts.utils.av

import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class OpusControlHeaderTest {
    @Test
    fun `test write`() {
        val opusControlHeader = OpusControlHeader(payloadSize = 515)
        val output = opusControlHeader.toByteBuffer()
        assertArrayEquals(
            byteArrayOf(
                0x7F, 0xE0.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x5
            ),
            output.toByteArray()
        )
    }
}