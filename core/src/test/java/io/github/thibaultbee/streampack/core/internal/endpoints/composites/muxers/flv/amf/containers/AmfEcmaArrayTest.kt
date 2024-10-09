package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.containers

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.AmfType
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AmfEcmaArrayTest {
    @Test
    fun `encode array with int test`() {
        val i = 4
        val a = AmfEcmaArray()
        a.add(i)
        val buffer = a.encode()

        val expectedArray = byteArrayOf(
            AmfType.ECMA_ARRAY.value, 0, 0, 0, 1, // Array header
            0, 0, 0, 4, // value
            0, 0, AmfType.OBJECT_END.value // Array footer
        )
        assertArrayEquals(
            expectedArray,
            buffer.toByteArray()
        )
    }
}