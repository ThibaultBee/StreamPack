package io.github.thibaultbee.streampack.core.streamer.file

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
import android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.net.Uri
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File

object VideoUtils {
    fun verifyFile(file: File) {
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    fun verify(
        context: Context,
        uri: Uri,
        audioConfig: AudioConfig?,
        videoConfig: VideoConfig?,
    ) {
        val metadataRetriever = MediaMetadataRetriever()
        try {
            metadataRetriever.setDataSource(context, uri)
            assertTrue(metadataRetriever.extractMetadata(METADATA_KEY_DURATION)!!.toInt() > 0)

            // Video
            if (videoConfig != null) {
                assertEquals("yes", metadataRetriever.extractMetadata(METADATA_KEY_HAS_VIDEO)!!)
                /**
                 * Warning: it doesn't work if device is rotated during the test
                 */
                if (context.isDevicePortrait) {
                    assertEquals(
                        videoConfig.resolution.height,
                        metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                    )
                    assertEquals(
                        videoConfig.resolution.width,
                        metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                    )
                } else {
                    assertEquals(
                        videoConfig.resolution.width,
                        metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                    )
                    assertEquals(
                        videoConfig.resolution.height,
                        metadataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                    )
                }
            }

            // Audio
            if (audioConfig != null) {
                assertEquals("yes", metadataRetriever.extractMetadata(METADATA_KEY_HAS_AUDIO)!!)
                assertEquals(
                    audioConfig.sampleRate,
                    metadataRetriever.extractMetadata(METADATA_KEY_SAMPLERATE)!!.toInt()
                )
            }

        } catch (e: Exception) {
            throw e
        } finally {
            metadataRetriever.release()
        }
    }
}