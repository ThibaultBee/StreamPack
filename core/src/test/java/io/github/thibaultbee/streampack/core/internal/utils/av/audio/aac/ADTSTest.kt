package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac

import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.core.internal.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ADTSTest {
    @Test
    fun `test ADTS with payload size of 378 bytes`() {
        val expectedAdts =
            ResourcesUtils.readByteBuffer("test-samples/audio/adts/adts-378bytes")

        val adts = ADTS(
            protectionAbsent = true, // No CRC protection
            sampleRate = 44100,
            channelCount = 2,
            payloadLength = 371
        )

        assertArrayEquals(
            expectedAdts.array(),
            adts.toByteBuffer().toByteArray().copyOfRange(0, 7)
        )
    }

    @Test
    fun `test ADTS with payload size of 516 bytes`() {
        val expectedAdts = ResourcesUtils.readByteBuffer("test-samples/audio/adts/adts-516bytes")
        val adts = ADTS(
            protectionAbsent = true, // No CRC protection
            sampleRate = 44100,
            channelCount = 2,
            payloadLength = 509
        )

        assertArrayEquals(
            expectedAdts.array(),
            adts.toByteBuffer().toByteArray().copyOfRange(0, 7)
        )
    }
}