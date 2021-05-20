package com.github.thibaultbee.streampack.utils

import android.media.MediaFormat

/**
 * Helper for audio/video codec.
 */
object CodecUtils {
    /**
     * Get supported video encoder list
     */
    val supportedVideoEncoder: List<String>
        /**
         * @return list of video mime type
         */
        get() = listOf(MediaFormat.MIMETYPE_VIDEO_AVC)

    /**
     * Get supported audio encoder list
     */
    val supportedAudioEncoder: List<String>
        /**
         * @return list of audio mime type
         */
        get() = listOf(MediaFormat.MIMETYPE_AUDIO_AAC)
}