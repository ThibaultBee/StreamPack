package io.github.thibaultbee.streampack.core.elements.encoders

import android.media.AudioFormat
import android.media.MediaFormat
import org.junit.Assert
import org.junit.Test

class AudioCodecConfigTest {
    /**
     * This test required [Size] which is not available in unit test.
     */
    @Test
    fun equalsConfigTest() {
        val audioCodecConfig = AudioCodecConfig()

        Assert.assertEquals(audioCodecConfig, audioCodecConfig)
        Assert.assertEquals(audioCodecConfig, AudioCodecConfig())
        Assert.assertNotEquals(
            audioCodecConfig,
            AudioCodecConfig(mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS)
        )
        Assert.assertNotEquals(audioCodecConfig, AudioCodecConfig(startBitrate = 1_000))
        Assert.assertNotEquals(audioCodecConfig, AudioCodecConfig(sampleRate = 48000))
        Assert.assertNotEquals(
            audioCodecConfig,
            AudioCodecConfig(channelConfig = AudioFormat.CHANNEL_IN_MONO)
        )
        Assert.assertNotEquals(
            audioCodecConfig,
            AudioCodecConfig(byteFormat = AudioFormat.ENCODING_PCM_8BIT)
        )
        Assert.assertNotEquals(audioCodecConfig, AudioCodecConfig(profile = 1234))
    }
}