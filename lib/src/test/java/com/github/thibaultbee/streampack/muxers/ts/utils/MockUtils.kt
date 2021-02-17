package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.muxers.IMuxerListener
import org.junit.Assert
import java.nio.ByteBuffer

/**
 * Assert expected buffer is equals to generated buffer
 * @param expectedBuffer expected buffer (pre-generated buffer)
 */
class AssertEqualsSingleBufferMockMuxerListener(private val expectedBuffer: ByteBuffer) :
    IMuxerListener {
    override fun onOutputFrame(buffer: ByteBuffer) {
        Assert.assertEquals(expectedBuffer, buffer)
    }
}

/**
 * Assert expected buffers is equals to generated buffers
 * @param expectedBuffers expected buffers (often pre-generated buffers)
 */
class AssertEqualsBuffersMockMuxerListener(private val expectedBuffers: List<ByteBuffer>) :
    IMuxerListener {
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