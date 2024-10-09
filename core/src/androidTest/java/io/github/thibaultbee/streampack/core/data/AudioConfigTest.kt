package io.github.thibaultbee.streampack.core.data

import android.media.AudioFormat
import android.media.MediaFormat
import android.util.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AudioConfigTest {
    /**
     * This test required [Size] which is not available in unit test.
     */
    @Test
    fun equalsConfigTest() {
        val audioConfig = AudioConfig()

        assertEquals(audioConfig, audioConfig)
        assertEquals(audioConfig, AudioConfig())
        assertNotEquals(audioConfig, AudioConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS))
        assertNotEquals(audioConfig, AudioConfig(startBitrate = 1_000))
        assertNotEquals(audioConfig, AudioConfig(sampleRate = 48000))
        assertNotEquals(audioConfig, AudioConfig(channelConfig = AudioFormat.CHANNEL_IN_MONO))
        assertNotEquals(
            audioConfig,
            AudioConfig(byteFormat = AudioFormat.ENCODING_PCM_8BIT)
        )
        assertNotEquals(audioConfig, AudioConfig(profile = 1234))
    }
}