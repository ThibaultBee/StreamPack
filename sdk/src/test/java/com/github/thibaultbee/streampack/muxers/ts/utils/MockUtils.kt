package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.muxers.IMuxerListener
import org.junit.Assert
import java.nio.ByteBuffer

/**
 * Assert expected buffer is equals to generated buffer
 * @param expectedBuffer expected buffer (pre-generated buffer)
 */
class AssertEqualsSingleBufferMockMuxerListener(private val expectedBuffer: ByteBuffer) :
    IMuxerListener {
    override fun onOutputFrame(packet: Packet) {
        Assert.assertEquals(expectedBuffer, packet.buffer)
    }
}

/**
 * Assert expected buffers is equals to generated buffers
 * @param expectedBuffers expected buffers (often pre-generated buffers)
 */
class AssertEqualsBuffersMockMuxerListener(private val expectedBuffers: List<ByteBuffer>) :
    IMuxerListener {
    var expectedBufferIndex = 0

    override fun onOutputFrame(packet: Packet) {
        // Do not compare adaptation field PCR -> Compare first part and last part separately.
        val expectedArray = expectedBuffers[expectedBufferIndex].array()
        val actualArray = packet.buffer.array()
        Assert.assertArrayEquals(
            "Headers not equal at index $expectedBufferIndex",
            expectedArray.copyOfRange(0, 6),
            actualArray.copyOfRange(0, 6)
        )
        Assert.assertArrayEquals(
            "Payloads not equal at index $expectedBufferIndex",
            expectedArray.copyOfRange(12, expectedBuffers[expectedBufferIndex].limit()),
            actualArray.copyOfRange(12, expectedBuffers[expectedBufferIndex].limit())
        )
        expectedBufferIndex++
    }
}