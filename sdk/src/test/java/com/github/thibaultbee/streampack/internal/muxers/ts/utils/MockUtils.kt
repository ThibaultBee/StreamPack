/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.internal.muxers.ts.utils

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.packets.TS
import com.github.thibaultbee.streampack.utils.extractArray
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
        val actualArray = packet.buffer.extractArray()
        var i = 0
        while (i < MuxerConst.MAX_OUTPUT_PACKET_NUMBER) {
            // Do not compare adaptation field PCR -> Compare first part and last part separately.
            val expectedArray = expectedBuffers[expectedBufferIndex].array()

            Assert.assertArrayEquals(
                "Headers not equal at index $expectedBufferIndex",
                expectedArray,
                actualArray.copyOfRange(
                    i * TS.PACKET_SIZE,
                    (i + 1) * TS.PACKET_SIZE
                )
            )

            expectedBufferIndex++
            i++

            if (expectedBufferIndex >= expectedBuffers.size) {
                return
            }
        }
    }
}