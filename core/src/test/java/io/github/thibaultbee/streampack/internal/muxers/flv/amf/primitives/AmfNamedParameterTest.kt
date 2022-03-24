package io.github.thibaultbee.streampack.internal.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.utils.extractArray
import org.junit.Assert.*
import org.junit.Test

class AmfNamedParameterTest {
    private fun encodeNamedParameterTest(name: String, v: Any) {
        val amfNamedParameter = AmfNamedParameter(name, v)
        val buffer = amfNamedParameter.encode()

        val expectedArray = byteArrayOf(
            0x0,
            amfNamedParameter.name.length.toByte()
        ) + amfNamedParameter.name.toByteArray() + amfNamedParameter.v.encode().extractArray()
        assertArrayEquals(
            expectedArray,
            buffer.extractArray()
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