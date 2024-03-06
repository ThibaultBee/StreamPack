package io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.AmfType
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

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
        assertArrayEquals(expectedArray, buffer.toByteArray()) // Remove direct part
    }
}