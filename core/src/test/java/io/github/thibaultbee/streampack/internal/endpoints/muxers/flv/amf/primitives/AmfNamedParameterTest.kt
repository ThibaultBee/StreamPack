package io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import org.junit.Assert.*
import org.junit.Test

class AmfNamedParameterTest {
    private fun encodeNamedParameterTest(name: String, v: Any) {
        val amfNamedParameter = AmfNamedParameter(name, v)
        val buffer = amfNamedParameter.encode()

        val expectedArray = byteArrayOf(
            0x0,
            amfNamedParameter.name.length.toByte()
        ) + amfNamedParameter.name.toByteArray() + amfNamedParameter.v.encode().toByteArray()
        assertArrayEquals(
            expectedArray,
            buffer.toByteArray()
        )
    }
    @Test
    fun `encode named boolean test`() {
        encodeNamedParameterTest("myBoolean", true)
    }

    @Test
    fun `encode named double test`() {
        encodeNamedParameterTest("myNumber", 4.0)
    }

    @Test
    fun `encode named string test`() {
        encodeNamedParameterTest("myString", "stringToEncode")
    }
}