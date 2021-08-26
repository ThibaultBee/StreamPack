package com.github.thibaultbee.streampack.data

import android.util.Range
import com.github.thibaultbee.streampack.streamers.CameraSrtLiveStreamer

/**
 * Bitrate regulator configuration. Use it to control bitrate
 *
 * @see [CameraSrtLiveStreamer]
 */
data class BitrateRegulatorConfig(
    /**
     * Video encoder bitrate ranges in bits/s. [Range.upper] is video target bitrate.
     */
    val videoBitrateRange: Range<Int>,
    /**
     * Audio encoder bitrate ranges in bits/s. [Range.upper] is video target bitrate.
     */
    val audioBitrateRange: Range<Int>
) {
    /**
     * Builder class for [BitrateRegulatorConfig] objects. Use this class to configure and create an [BitrateRegulatorConfig] instance.
     */
    data class Builder(
        private var videoBitrateRange: Range<Int> = Range(500, 10000),
        private var audioBitrateRange: Range<Int> = Range(128000, 128000)
    ) {
        /**
         * Set video bitrate range. Upper boundary is the target bitrate.
         *
         * @param videoBitrateRange video bitrate regulator limits
         */
        fun setVideoBitrateRange(videoBitrateRange: Range<Int>) =
            apply { this.videoBitrateRange = videoBitrateRange }

        /**
         * Set audio bitrate range. Upper boundary is the target bitrate.
         *
         * @param audioBitrateRange audio bitrate regulator limits
         */
        fun setAudioBitrateRange(audioBitrateRange: Range<Int>) =
            apply { this.audioBitrateRange = audioBitrateRange }

        /**
         * Combines all of the characteristics that have been set and return a new [BitrateRegulatorConfig] object.
         *
         * @return a new [BitrateRegulatorConfig] object
         */
        fun build() =
            BitrateRegulatorConfig(videoBitrateRange, audioBitrateRange)
    }
}
