package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.amf.AmfType
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AmfStringTest {
    @Test
    fun `encode string test`() {
        val s = "stringToEncode"
        val amfString = AmfString(s)
        val buffer = amfString.encode()

        val expectedArray =
            byteArrayOf(AmfType.STRING.value, 0, s.length.toByte()) + s.toByteArray()
        assertArrayEquals(
            expectedArray,
            buffer.toByteArray()
        )
    }
}