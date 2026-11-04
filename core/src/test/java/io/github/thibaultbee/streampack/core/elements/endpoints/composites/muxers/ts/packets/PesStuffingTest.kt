/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.packets

import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.data.Packet
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxerInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.Stream
import io.github.thibaultbee.streampack.core.elements.utils.FakeFrameFactory
import io.github.thibaultbee.streampack.core.elements.utils.MockUtils
import io.github.thibaultbee.streampack.core.elements.utils.pool.ByteBufferPool
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.random.Random

/**
 * Muxes PES of every size and parses the TS back, so a payload that fits in the
 * first packet (where stuffing must grow the existing adaptation field) is covered.
 */
class PesStuffingTest {
    private class CollectingListener : IMuxerInternal.IMuxerListener {
        val bytes = ByteArrayOutputStream()
        override fun onOutputFrame(packet: Packet) {
            val b = packet.buffer.duplicate()
            b.rewind()
            val array = ByteArray(b.remaining())
            b.get(array)
            bytes.write(array)
        }
    }

    private fun u8(b: Byte) = b.toInt() and 0xFF

    private fun checkPes(
        isVideo: Boolean,
        hasPcr: Boolean,
        isKeyFrame: Boolean,
        size: Int
    ) {
        val case = "video=$isVideo pcr=$hasPcr key=$isKeyFrame size=$size"
        val pid: Short = 256
        val input = Random.nextBytes(size)
        val frame = FakeFrameFactory.create(
            mimeType = if (isVideo) MediaFormat.MIMETYPE_VIDEO_AVC else MediaFormat.MIMETYPE_AUDIO_AAC,
            buffer = ByteBuffer.wrap(input.copyOf()),
            pts = 1_400_000,
            dts = if (isVideo) 1_366_666 else null,
            isKeyFrame = isKeyFrame
        )
        val config = if (isVideo) {
            VideoCodecConfig(
                resolution = MockUtils.mockSize(640, 480),
                profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )
        } else {
            AudioCodecConfig()
        }
        val listener = CollectingListener()
        Pes(ByteBufferPool(true), listener, Stream(config, pid), hasPcr).write(frame)

        val ts = listener.bytes.toByteArray()
        assertTrue("$case: empty output", ts.isNotEmpty())
        assertEquals("$case: output is not whole packets", 0, ts.size % TS.PACKET_SIZE)

        val pes = ByteArrayOutputStream()
        for (n in 0 until ts.size / TS.PACKET_SIZE) {
            val p = n * TS.PACKET_SIZE
            val at = "$case packet=$n"
            assertEquals("$at: sync byte", TS.SYNC_BYTE, ts[p])
            assertEquals("$at: PUSI", n == 0, (u8(ts[p + 1]) and 0x40) != 0)
            assertEquals("$at: PID", pid.toInt(), ((u8(ts[p + 1]) and 0x1F) shl 8) or u8(ts[p + 2]))
            assertEquals("$at: continuity counter", n and 0xF, u8(ts[p + 3]) and 0xF)
            val afc = (u8(ts[p + 3]) shr 4) and 0x3
            assertTrue("$at: payload flag missing (AFC=$afc)", (afc and 0x1) != 0)

            var payloadStart = p + 4
            if ((afc and 0x2) != 0) {
                val afLength = u8(ts[p + 4])
                payloadStart = p + 5 + afLength
                assertTrue("$at: AF length $afLength overruns packet", payloadStart <= p + TS.PACKET_SIZE)
                if (n == 0) {
                    assertTrue("$at: first packet AF has no flags byte", afLength >= 1)
                }
                if (afLength >= 1) {
                    val flags = u8(ts[p + 5])
                    var fieldsEnd = p + 6
                    if (n == 0) {
                        assertEquals("$at: random access", isKeyFrame, (flags and 0x40) != 0)
                        assertEquals("$at: PCR flag", hasPcr, (flags and 0x10) != 0)
                        if (hasPcr) fieldsEnd += 6
                    } else {
                        assertEquals("$at: continuation AF flags", 0, flags)
                    }
                    assertTrue("$at: AF too short for its fields", fieldsEnd <= payloadStart)
                    for (i in fieldsEnd until payloadStart) {
                        assertEquals("$at: stuffing byte at ${i - p}", 0xFF, u8(ts[i]))
                    }
                }
            } else if (n == 0) {
                throw AssertionError("$at: first packet lost its adaptation field")
            }
            pes.write(ts, payloadStart, p + TS.PACKET_SIZE - payloadStart)
        }

        val pesBytes = pes.toByteArray()
        assertTrue("$case: PES too short", pesBytes.size >= 9)
        assertEquals("$case: PES start code", 0x000001,
            (u8(pesBytes[0]) shl 16) or (u8(pesBytes[1]) shl 8) or u8(pesBytes[2]))
        assertEquals("$case: stream id", if (isVideo) 0xE0 else 0xC0, u8(pesBytes[3]))
        val headerDataLength = u8(pesBytes[8])
        assertEquals("$case: PES header data length", if (isVideo) 10 else 5, headerDataLength)
        val pesPacketLength = (u8(pesBytes[4]) shl 8) or u8(pesBytes[5])
        assertEquals("$case: PES packet length", 3 + headerDataLength + size, pesPacketLength)
        assertEquals("$case: PES size", 6 + pesPacketLength, pesBytes.size)
        assertArrayEquals("$case: payload", input, pesBytes.copyOfRange(9 + headerDataLength, pesBytes.size))
    }

    @Test
    fun `PES of every size round-trips through TS`() {
        MockUtils.mockTimeUtils(700_000)
        val failures = mutableListOf<String>()
        for (isVideo in listOf(true, false)) {
            for (hasPcr in listOf(true, false)) {
                for (isKeyFrame in listOf(true, false)) {
                    for (size in 1..400) {
                        try {
                            checkPes(isVideo, hasPcr, isKeyFrame, size)
                        } catch (e: AssertionError) {
                            failures.add(e.message ?: e.toString())
                        }
                    }
                }
            }
        }
        assertTrue(
            "${failures.size} of 3200 cases failed, first: ${failures.take(5).joinToString("; ")}",
            failures.isEmpty()
        )
    }
}
