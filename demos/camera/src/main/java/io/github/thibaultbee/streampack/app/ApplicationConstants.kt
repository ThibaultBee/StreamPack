package io.github.thibaultbee.streampack.app

import android.content.pm.ActivityInfo
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig

/**
 * Application configuration.
 */
object ApplicationConstants {
    /**
     * Default application orientation.
     * Also set in `AndroidManifest.xml` `android:screenOrientation` attribute.
     */
    const val supportedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    /**
     * User preferences file name.
     */
    const val userPrefName = "settings"

    /**
     * Video default configuration.
     */
    object Video {
        private val defaultVideoConfig = VideoConfig()

        /**
         * Default video encoder.
         */
        val defaultEncoder = defaultVideoConfig.mimeType

        /**
         * Default FPS for live stream.
         */
        val defaultFps = defaultVideoConfig.fps

        /**
         * Default bitrate for live stream.
         */
        val defaultBitrateInBps = defaultVideoConfig.startBitrate

        /**
         * Default resolution for live stream.
         */
        val defaultResolution = defaultVideoConfig.resolution
    }

    /**
     * Audio default configuration.
     */
    object Audio {
        private val defaultAudioConfig = AudioConfig()

        /**
         * Default audio encoder.
         */
        val defaultEncoder = defaultAudioConfig.mimeType

        /**
         * Default channel config.
         */
        val defaultChannelConfig = defaultAudioConfig.channelConfig

        /**
         * Default start bitrate.
         */
        val defaultBitrateInBps = defaultAudioConfig.startBitrate

        /**
         * Default sample rate.
         */
        val defaultSampleRate = defaultAudioConfig.sampleRate

        /**
         * Default byte format.
         */
        val defaultByteFormat = defaultAudioConfig.byteFormat
    }

    object Connection {
        /**
         * Default latency in milliseconds.
         */
        const val defaultUri = "srt://192.168.1.11:9998"
    }
}
