package io.github.thibaultbee.streampack.core.data

import android.media.MediaFormat
import android.util.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoConfigTest {
    /**
     * This test required [Size] which is not available in unit test.
     */
    @Test
    fun equalsConfigTest() {
        val videoConfig = VideoConfig()

        assertEquals(videoConfig, videoConfig)
        assertEquals(videoConfig, VideoConfig())
        assertNotEquals(videoConfig, VideoConfig(mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC))
        assertNotEquals(videoConfig, VideoConfig(startBitrate = 1_000))
        assertNotEquals(videoConfig, VideoConfig(resolution = Size(1920, 1080)))
        assertNotEquals(videoConfig, VideoConfig(fps = 15))
        assertNotEquals(
            videoConfig,
            VideoConfig(profile = 1234, level = videoConfig.level)
        ) // Level is inferred from profile
        assertNotEquals(videoConfig, VideoConfig(level = 1234))
        assertNotEquals(videoConfig, VideoConfig(gopDuration = 1234f))
    }
}