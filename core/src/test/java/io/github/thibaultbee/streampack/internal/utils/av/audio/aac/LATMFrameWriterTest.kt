package io.github.thibaultbee.streampack.internal.utils.av.audio.aac

import android.media.AudioFormat
import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.internal.utils.av.audio.AudioSpecificConfig
import io.github.thibaultbee.streampack.internal.utils.extensions.extractArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.nio.ByteBuffer

class LATMFrameWriterTest {
    @Test
    fun `test AAC-LC 44100Hz mono with payload`() {
        val expectedLatm = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-lc-44100Hz-mono/aac.latm")
        )

        val payload = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-lc-44100Hz-mono/frame.raw")
        )

        val config = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )

        val esds = AudioSpecificConfig.fromAudioConfig(config).toByteBuffer()
        val latm = LATMFrameWriter.fromEsds(payload, esds)
        assertArrayEquals(
            expectedLatm.array(),
            latm.toByteBuffer().extractArray()
        )
    }

    @Test
    fun `test AAC-HE 44100Hz mono with payload`() {
        val expectedLatm = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-he-44100Hz-mono/aac.latm")
        )

        val esds = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-he-44100Hz-mono/esds.raw")
        )
        val payload = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-he-44100Hz-mono/frame.raw")
        )

        val latm = LATMFrameWriter.fromEsds(payload, esds)
        assertArrayEquals(
            expectedLatm.array(),
            latm.toByteBuffer().extractArray()
        )
    }

    @Test
    fun `test AAC-HEv2 44100Hz stereo with payload`() {
        val expectedLatm = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-hev2-44100Hz-stereo/aac.latm")
        )

        val esds = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-hev2-44100Hz-stereo/esds.raw")
        )
        val payload = ByteBuffer.wrap(
            ResourcesUtils.readResources("test-samples/audio/latm/aac-hev2-44100Hz-stereo/frame.raw")
        )

        val latm = LATMFrameWriter.fromEsds(payload, esds)
        assertArrayEquals(
            expectedLatm.array(),
            latm.toByteBuffer().extractArray()
        )
    }
}
