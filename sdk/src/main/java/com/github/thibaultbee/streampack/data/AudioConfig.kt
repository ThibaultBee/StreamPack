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
     *
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    val byteFormat: Int,

    /**
     * Enable/disable audio echo canceller.
     * If device does not have an echo canceller, it does nothing.
     */
    val enableEchoCanceler: Boolean,

    /**
     * Enable/disable audio noise suppressor.
     * If device does not have a noise suppressor, it does nothing.
     */
    val enableNoiseSuppressor: Boolean
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
        fun getNumberOfChannels(channelConfig: Int) = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw InvalidParameterException("Audio format not supported: $channelConfig")
        }

        /**
         * Returns channel configuration from the number of channels.
         *
         * @param nChannel 1 for mono, 2 for stereo
         * @return channel configuration (either [AudioFormat.CHANNEL_IN_MONO] or [AudioFormat.CHANNEL_IN_STEREO])
         */
        fun getChannelConfig(nChannel: Int) = when (nChannel) {
            1 -> AudioFormat.CHANNEL_IN_MONO
            2 -> AudioFormat.CHANNEL_IN_STEREO
            else -> throw InvalidParameterException("Number of channels not supported: $nChannel")
        }
    }

    /**
     * Builder class for [AudioConfig] objects. Use this class to configure and create an [AudioConfig] instance.
     */
    data class Builder(
        private var mimeType: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        private var startBitrate: Int = 128000,
        private var sampleRate: Int = 44100,
        private var nChannel: Int = 2,
        private var byteFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
        private var enableEchoCanceler: Boolean = false,
        private var enableNoiseSuppressor: Boolean = false
    ) {
        /**
         * Set audio encoder mime type.
         *
         * @param mimeType audio encoder mime type from [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
         */
        fun setMimeType(mimeType: String) = apply { this.mimeType = mimeType }

        /**
         * Set audio encoder bitrate.
         *
         * @param startBitrate audio encoder bitrate in bits/s.
         */
        fun setStartBitrate(startBitrate: Int) = apply { this.startBitrate = startBitrate }

        /**
         * Set sample rate.
         *
         * @param sampleRate audio capture sample rate (example: 44100, 48000,...)
         */
        fun setSampleRate(sampleRate: Int) = apply { this.sampleRate = sampleRate }

        /**
         * Set number of channels.
         *
         * @param nChannel 1 for mono, 2 for stereo
         */
        fun setNumberOfChannel(nChannel: Int) = apply { this.nChannel = nChannel }

        /**
         * Set audio capture byte format.
         *
         * @param byteFormat [AudioFormat.ENCODING_PCM_8BIT], [AudioFormat.ENCODING_PCM_16BIT] or [AudioFormat.ENCODING_PCM_FLOAT]
         */
        fun setByteFormat(byteFormat: Int) = apply { this.byteFormat = byteFormat }

        /**
         * Enable echo canceller.
         * If device does not have an echo canceller, it does nothing.
         * @see [setEchoCanceler]
         */
        fun enableEchoCanceler() = apply { this.enableEchoCanceler = true }

        /**
         * Enable/disable echo canceller.
         * If device does not have an echo canceller, it does nothing.
         * @see [enableEchoCanceler]
         */
        fun setEchoCanceler(enable: Boolean) = apply { this.enableEchoCanceler = enable }

        /**
         * Enable noise suppressor.
         * If device does not have a noise suppressor, it does nothing.
         * @see [setNoiseSuppressor]
         */
        fun enableNoiseSuppressor() = apply { this.enableNoiseSuppressor = true }

        /**
         * Enable/disable noise suppressor.
         * If device does not have a noise suppressor, it does nothing.
         * @see [enableNoiseSuppressor]
         */
        fun setNoiseSuppressor(enable: Boolean) = apply { this.enableNoiseSuppressor = enable }

        /**
         * Combines all of the characteristics that have been set and return a new [AudioConfig] object.
         *
         * @return a new [AudioConfig] object
         */
        fun build() =
            AudioConfig(
                mimeType,
                startBitrate,
                sampleRate,
                getChannelConfig(nChannel),
                byteFormat,
                enableEchoCanceler,
                enableNoiseSuppressor
            )
    }
}
