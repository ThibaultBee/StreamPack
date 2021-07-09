/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.data

import android.media.AudioFormat
import android.media.MediaFormat
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer
import com.github.thibaultbee.streampack.utils.isAudio
import java.security.InvalidParameterException

/**
 * Audio configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [BaseCameraStreamer.configure]
 */
data class AudioConfig(
    /**
     * Audio encoder mime type.
     * Only [MediaFormat.MIMETYPE_AUDIO_AAC] is supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String,

    /**
     * Audio encoder bitrate in bits/s.
     */
    val startBitrate: Int,

    /**
     * Audio capture sample rate in Hz.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): "44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices."
     */
    val sampleRate: Int,

    /**
     * Audio channel configuration.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): " AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices."
     *
     * @see [AudioFormat.CHANNEL_IN_MONO]
     * @see [AudioFormat.CHANNEL_IN_STEREO]
     */
    val channelConfig: Int,

    /**
     * Audio byte format.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): " AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices."
     *
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    val byteFormat: Int
) {
    init {
        require(mimeType.isAudio()) { "Mime Type must be audio" }
    }

    companion object {
        /**
         * Returns number of channels from a channel configuration.
         *
         * @param channelConfig [AudioFormat.CHANNEL_IN_MONO] or [AudioFormat.CHANNEL_IN_STEREO]
         * @return number of channels
         */
        fun getChannelNumber(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw InvalidParameterException("Unknown audio format: $channelConfig")
        }
    }
}
