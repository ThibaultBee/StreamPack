package com.github.thibaultbee.streampack.internal.muxers.flv.amf.primitives

import com.github.thibaultbee.streampack.internal.muxers.flv.amf.primitives.AmfBoolean
import com.github.thibaultbee.streampack.utils.extractArray
import org.junit.Assert.*
import org.junit.Test
import video.api.rtmpdroid.amf.AmfType

class AmfBooleanTest {
    @Test
    fun `encode boolean test`() {
        val b = true
        val amfBoolean = AmfBoolean(b)
        val buffer = amfBoolean.encode()

        val expectedArray = byteArrayOf(
            AmfType.BOOLEAN.value, if (b) {
                1
            } else {
                0
            }
        )
        assertArrayEquals(expectedArray, buffer.extractArray()) // Remove direct part
    }
}