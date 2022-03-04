package com.github.thibaultbee.streampack.internal.muxers.flv.amf.primitives

import com.github.thibaultbee.streampack.utils.extractArray
import org.junit.Assert.*
import org.junit.Test
import video.api.rtmpdroid.amf.AmfType

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
            buffer.extractArray()
        )
    }
}