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
package com.github.thibaultbee.streampack.internal.muxers.ts.packets

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.descriptors.AdaptationField
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.random.Random

class TransportStreamTest {
    class MockMuxerListener(
        private val expectedBuffer: ByteBuffer,
        private val numExpectedBuffer: Int = 1
    ) : IMuxerListener {
        private var nBuffer = 0
        override fun onOutputFrame(packet: Packet) {
            assertEquals(TS.PACKET_SIZE * numExpectedBuffer, packet.buffer.limit())
            assertEquals(TS.SYNC_BYTE, packet.buffer[0])
            for (i in 0 until min(packet.buffer.limit() - 4, expectedBuffer.remaining())) {
                assertEquals(
                    "Value is not equal on expectedBuffer.position ${expectedBuffer.position()}",
                    expectedBuffer.get(),
                    packet.buffer[i + 4 * (1 + i / (TS.PACKET_SIZE - 4))]
                )  /* Drop header */
            }
            nBuffer++
        }
    }

    class MockTransportStream(
        muxerListener: IMuxerListener,
        pid: Short = Random.nextInt().toShort()
    ) :
        TS(muxerListener, pid) {
        fun mockWrite(
            payload: ByteBuffer? = null,
            adaptationField: ByteBuffer? = null,
            specificHeader: ByteBuffer? = null
        ) = write(payload, adaptationField, specificHeader)
    }

    /**
     * Generates a randomized ByteBuffer
     * @param size size of buffer to generates
     * @return random ByteBuffer
     */
    private fun generateRandomBuffer(size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)
        buffer.put(Random.nextBytes(size))
        buffer.rewind()
        return buffer
    }

    @Test
    fun oneSmallPayloadTest() {
        val payload = generateRandomBuffer(10)
        val listener = MockMuxerListener(payload.duplicate())

        val transportStream =
            MockTransportStream(muxerListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun oneLargePayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE - 4) // 4 - header size
        val listener = MockMuxerListener(payload.duplicate())

        val transportStream =
            MockTransportStream(muxerListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun twoPayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE - 3) // 4 - header size
        val listener = MockMuxerListener(payload.duplicate(), 2)

        val transportStream =
            MockTransportStream(muxerListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun adaptationHeaderAndPayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE)
        val adaptationField = AdaptationField()
        val header = generateRandomBuffer(20)

        val expectedBuffer =
            ByteBuffer.allocate(payload.limit() + header.limit() + adaptationField.size)
        expectedBuffer.put(adaptationField.toByteBuffer())
        expectedBuffer.put(header)
        expectedBuffer.put(payload)
        val listener = MockMuxerListener(expectedBuffer, 2)

        payload.rewind()
        header.rewind()

        val transportStream =
            MockTransportStream(muxerListener = listener)
        transportStream.mockWrite(payload, adaptationField.toByteBuffer(), header)
    }
}