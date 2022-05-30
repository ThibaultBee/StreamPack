package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SyncSampleBoxTest {
    @Test
    fun `write valid stss test`() {
        val expectedBuffer = ResourcesUtils.readMP4ByteBuffer("stss.box")
        val stss = SyncSampleBox(
            listOf(
                1,
                31,
                61,
                91,
                121,
                151,
                181,
                211,
                241,
                271,
                301,
                331,
                361,
                391,
                421,
                451,
                481,
                511,
                541,
                571,
                601,
                631,
                661,
                691,
                721,
                751,
                781,
                811,
                841,
                871,
                901,
                931,
                961,
                991,
                1021,
                1051,
                1081,
                1111,
                1141,
                1171,
                1201,
                1231,
                1261,
                1291,
                1321
            )
        )
        val buffer = stss.write()
        assertArrayEquals(expectedBuffer.extractArray(), buffer.extractArray())
    }
}