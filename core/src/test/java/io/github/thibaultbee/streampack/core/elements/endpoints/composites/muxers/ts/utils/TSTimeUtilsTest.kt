package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TSTimeUtilsTest {
    @Test
    fun `test computeTimestamp90kHz with very large timestamp avoiding overflow`() {
        // 400,000,000,000 µs (about 111 hours)
        val timestamp = 400_000_000_000L
        val expectedPcrBase = 1_640_261_632L
        assertEquals(expectedPcrBase, TSTimeUtils.computeTimestamp90kHz(timestamp))
    }

    @Test
    fun `test computePcrExt with very large timestamp avoiding overflow`() {
        val timestamp = 400_000_000_000L
        val expectedPcrExt = 0L 
        assertEquals(expectedPcrExt, TSTimeUtils.computePcrExt(timestamp))
    }
}
