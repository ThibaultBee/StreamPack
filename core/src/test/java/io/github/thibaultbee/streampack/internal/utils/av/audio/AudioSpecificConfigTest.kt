package io.github.thibaultbee.streampack.internal.utils.av.audio

import io.github.thibaultbee.streampack.internal.utils.av.audio.aac.config.GASpecificConfig
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class AudioSpecificConfigTest {
    @Test
    fun `test AAC-LC 44100Hz stereo write AudioSpecificConfig`() {
        val expectedAudioSpecificConfig = byteArrayOf(0x12, 0x10)

        val audioObjectType = AudioObjectType.AAC_LC
        val channelConfiguration = ChannelConfiguration.CHANNEL_2

        val specificConfig = GASpecificConfig(
            audioObjectType,
            channelConfiguration,
            frameLengthFlag = false,
            dependsOnCodeCoder = false,
            extensionFlag = false
        )
        val audioSpecificConfig =
            AudioSpecificConfig(
                audioObjectType,
                44100,
                channelConfiguration,
                specificConfig = specificConfig
            )
        assertArrayEquals(expectedAudioSpecificConfig, audioSpecificConfig.toByteBuffer().array())
    }

    @Test
    fun `test AAC-LC 48000Hz stereo write AudioSpecificConfig`() {
        val expectedAudioSpecificConfig = byteArrayOf(0x11, 0x90.toByte())

        val audioObjectType = AudioObjectType.AAC_LC
        val channelConfiguration = ChannelConfiguration.CHANNEL_2

        val specificConfig = GASpecificConfig(
            audioObjectType,
            channelConfiguration,
            frameLengthFlag = false,
            dependsOnCodeCoder = false,
            extensionFlag = false
        )
        val audioSpecificConfig =
            AudioSpecificConfig(
                audioObjectType,
                48000,
                channelConfiguration,
                specificConfig = specificConfig
            )
        assertArrayEquals(expectedAudioSpecificConfig, audioSpecificConfig.toByteBuffer().array())
    }

    @Test
    fun `test AAC-LC 48000Hz stereo parse AudioSpecificConfig`() {
        val audioSpecificConfigArray = byteArrayOf(0x11, 0x90.toByte())

        val audioSpecificConfig =
            AudioSpecificConfig.parse(ByteBuffer.wrap(audioSpecificConfigArray))

        assertEquals(AudioObjectType.AAC_LC, audioSpecificConfig.audioObjectType)
        assertEquals(48000, audioSpecificConfig.sampleRate)
        assertEquals(ChannelConfiguration.CHANNEL_2, audioSpecificConfig.channelConfiguration)
        assertNull(audioSpecificConfig.extension)
        assertNotNull(audioSpecificConfig.specificConfig)
        assertTrue(audioSpecificConfig.specificConfig is GASpecificConfig)
        val gaSpecificConfig = audioSpecificConfig.specificConfig as GASpecificConfig
        assertEquals(false, gaSpecificConfig.frameLengthFlag)
        assertEquals(false, gaSpecificConfig.dependsOnCodeCoder)
        assertEquals(false, gaSpecificConfig.extensionFlag)
    }

    @Test
    fun `test AAC-HE 48000Hz stereo parse AudioSpecificConfig`() {
        val audioSpecificConfigArray = byteArrayOf(0x2B, 0x92.toByte(), 0x08, 0x00)

        val audioSpecificConfig =
            AudioSpecificConfig.parse(ByteBuffer.wrap(audioSpecificConfigArray))

        assertEquals(AudioObjectType.AAC_LC, audioSpecificConfig.audioObjectType)
        assertEquals(22050, audioSpecificConfig.sampleRate)
        assertEquals(ChannelConfiguration.CHANNEL_2, audioSpecificConfig.channelConfiguration)
        assertNotNull(audioSpecificConfig.extension)
        assertEquals(AudioObjectType.SBR, audioSpecificConfig.extension!!.extensionAudioObjectType)
        assertEquals(44100, audioSpecificConfig.extension!!.sampleRate)
        assertNotNull(audioSpecificConfig.specificConfig)
    }
}