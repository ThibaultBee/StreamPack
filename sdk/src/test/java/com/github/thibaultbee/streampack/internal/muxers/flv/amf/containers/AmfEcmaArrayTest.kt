package com.github.thibaultbee.streampack.internal.muxers.flv.amf.containers

import com.github.thibaultbee.streampack.internal.muxers.flv.amf.containers.AmfEcmaArray
import com.github.thibaultbee.streampack.utils.extractArray
import org.junit.Assert.*
import org.junit.Test
import video.api.rtmpdroid.amf.AmfType

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
            buffer.extractArray()
        )
    }
}