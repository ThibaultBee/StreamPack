package io.github.thibaultbee.streampack.internal.utils.av.audio.aac

import io.github.thibaultbee.streampack.utils.ResourcesUtils
import io.github.thibaultbee.streampack.utils.extractArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class ADTSTest {
    @Test
    fun `test ADTS with payload size of 378 bytes`() {
        val expectedAdts = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/encoders/adts-378bytes")
        )
        val adts = ADTS(
            protectionAbsent = true, // No CRC protection
            sampleRate = 44100,
            channelCount = 2,
            payloadLength = 371
        )

        assertArrayEquals(
            expectedAdts.array(),
            adts.toByteBuffer().extractArray().copyOfRange(0, 7)
        )
    }

    @Test
    fun `test ADTS with payload size of 516 bytes`() {
        val expectedAdts = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/encoders/adts-516bytes")
        )
        val adts = ADTS(
            protectionAbsent = true, // No CRC protection
            sampleRate = 44100,
            channelCount = 2,
            payloadLength = 509
        )

        assertArrayEquals(
            expectedAdts.array(),
            adts.toByteBuffer().extractArray().copyOfRange(0, 7)
        )
    }
}