package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.amf.primitives

import org.junit.Assert.*
import org.junit.Test

class AmfInt32Test {

    @Test
    fun `encode int test`() {
        val i = 4
        val amfInt32 =  AmfInt32(i)
        val buffer = amfInt32.encode()

        assertEquals(i, buffer.getInt(0))
    }
}