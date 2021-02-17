package com.github.thibaultbee.streampack.data

import android.media.AudioFormat
import java.security.InvalidParameterException

data class AudioConfig(
    var mimeType: String,
    var startBitrate: Int,
    var sampleRate: Int,
    var channelConfig: Int,
    var audioByteFormat: Int
) {
    companion object {
        fun getChannelNumber(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw InvalidParameterException("Unknown audio format: $channelConfig")
        }
    }
}
