package com.github.thibaultbee.streampack.sources

import com.github.thibaultbee.streampack.utils.FakeLogger
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class AudioCaptureUnitTest {
    @Test
    fun `assert exception on bad state`() {
        val audioCapture = AudioCapture(FakeLogger())
        try {
            audioCapture.run()
            Assert.fail()
        } catch (e: Exception) {
        }
        try {
            audioCapture.getFrame(ByteBuffer.allocate(10))
            Assert.fail()
        } catch (e: Exception) {
        }
    }

    @Test
    fun `assert no exception on bad state`() {
        val audioCapture = AudioCapture(FakeLogger())
        try {
            audioCapture.stop()
        } catch (e: Exception) {
            Assert.fail()
        }
        try {
            audioCapture.close()
        } catch (e: Exception) {
            Assert.fail()
        }
    }
}