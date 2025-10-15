package io.github.thibaultbee.streampack.core.elements.encoders

import android.media.MediaFormat
import android.util.Size
import org.junit.Assert
import org.junit.Test

class VideoCodecConfigTest {
    /**
     * This test required [Size] which is not available in unit test.
     */
    @Test
    fun equalsConfigTest() {
        val videoConfig = VideoCodecConfig()

        Assert.assertEquals(videoConfig, videoConfig)
        Assert.assertEquals(videoConfig, VideoCodecConfig())
        Assert.assertNotEquals(
            videoConfig,
            VideoCodecConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC)
        )
        Assert.assertNotEquals(videoConfig, VideoCodecConfig(startBitrate = 1_000))
        Assert.assertNotEquals(videoConfig, VideoCodecConfig(resolution = Size(1920, 1080)))
        Assert.assertNotEquals(videoConfig, VideoCodecConfig(fps = 15))
        Assert.assertNotEquals(
            videoConfig,
            VideoCodecConfig(profile = 1234, level = videoConfig.level)
        )
        Assert.assertNotEquals(
            videoConfig,
            VideoCodecConfig(
                profile = videoConfig.profile,
                level = 1234
            )
        )
        Assert.assertNotEquals(videoConfig, VideoCodecConfig(gopDurationInS = 1234f))
    }
}