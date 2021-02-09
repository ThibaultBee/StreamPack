package com.github.thibaultbee.srtstreamer.mux.ts.utils

import com.github.thibaultbee.srtstreamer.interfaces.MuxListener
import org.junit.Assert
import java.nio.ByteBuffer

/**
 * Assert expected buffer is equals to generated buffer
 * @param expectedBuffer expected buffer (pre-generated buffer)
 */
class AssertEqualsSingleBufferMockMuxListener(private val expectedBuffer: ByteBuffer) :
    MuxListener {
    override fun onOutputFrame(buffer: ByteBuffer) {
        Assert.assertEquals(expectedBuffer, buffer)
    }
}

/**
 * Assert expected buffers is equals to generated buffers
 * @param expectedBuffers expected buffers (often pre-generated buffers)
 */
class AssertEqualsBuffersMockMuxListener(private val expectedBuffers: List<ByteBuffer>) :
    MuxListener {
    var expectedBufferIndex = 0

    override fun onOutputFrame(buffer: ByteBuffer) {
        Assert.assertEquals(
            "Not equals at index $expectedBufferIndex",
            expectedBuffers[expectedBufferIndex],
            buffer
        )
        expectedBufferIndex++
    }
}