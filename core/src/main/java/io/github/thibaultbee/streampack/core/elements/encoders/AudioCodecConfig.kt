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
package io.github.thibaultbee.streampack.core.elements.encoders

import android.media.AudioFormat
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.core.elements.utils.ByteFormatValue
import io.github.thibaultbee.streampack.core.elements.utils.ChannelConfigValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isAudio
import java.security.InvalidParameterException

/**
 * Audio configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 */
open class AudioCodecConfig(
    /**
     * Audio encoder mime type.
     *
     * **See Also:** [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    mimeType: String = MediaFormat.MIMETYPE_AUDIO_AAC,

    /**
     * Audio encoder bitrate in bits/s.
     */
    startBitrate: Int = 128_000,

    /**
     * Audio capture sample rate in Hz.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): "44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices."
     */
    val sampleRate: Int = getDefaultSampleRate(mimeType),

    /**
     * Audio channel configuration.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): " AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices."
     *
     * @see [AudioFormat.CHANNEL_IN_MONO]
     * @see [AudioFormat.CHANNEL_IN_STEREO]
     */
    @ChannelConfigValue val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,

    /**
     * Audio byte format.
     *
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    @ByteFormatValue val byteFormat: Int = AudioFormat.ENCODING_PCM_16BIT,

    /**
     * Audio profile.
     * For AAC only.
     * Default value is [MediaCodecInfo.CodecProfileLevel.AACObjectLC].
     *
     * @see [MediaCodecInfo.CodecProfileLevel.AACObjectLC]
     * @see [MediaCodecInfo.CodecProfileLevel.AACObjectHE]
     */
    profile: Int = getDefaultProfile(mimeType),
) : CodecConfig(mimeType, startBitrate, profile) {
    init {
        require(mimeType.isAudio) { "MimeType must be audio" }
        if (mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            if (profile == MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS) {
                require(channelConfig == AudioFormat.CHANNEL_IN_STEREO) { "AACObjectHE_PS only supports stereo" }
            }
        }
        if (mimeType == MediaFormat.MIMETYPE_AUDIO_AAC_HE_V2) {
            require(channelConfig == AudioFormat.CHANNEL_IN_STEREO) { "AAC_HE_V2 only supports stereo" }
        }
        if (mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS) {
            require(opusSupportedSamplingRate.contains(sampleRate)) {
                " Opus only supports ${
                    opusSupportedSamplingRate.joinToString(
                        ", "
                    )
                }"
            }
        }
    }

    /**
     * Get the media format from the audio configuration
     *
     * @return the corresponding audio media format
     */
    override fun getFormat(withProfileLevel: Boolean): MediaFormat {
        val format = MediaFormat.createAudioFormat(
            mimeType, sampleRate, getNumberOfChannels(channelConfig)
        )

        // Extended audio format
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            format.setInteger(
                MediaFormat.KEY_PCM_ENCODING, byteFormat
            )
        }
        format.setInteger(MediaFormat.KEY_BIT_RATE, startBitrate)

        if (withProfileLevel) {
            if (mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                format.setInteger(
                    MediaFormat.KEY_AAC_PROFILE, profile
                )
            }
        }

        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)

        return format
    }

    /**
     * Copies the audio configuration with new values.
     */
    fun copy(
        mimeType: String = this.mimeType,
        startBitrate: Int = this.startBitrate,
        sampleRate: Int = this.sampleRate,
        channelConfig: Int = this.channelConfig,
        byteFormat: Int = this.byteFormat,
        profile: Int = this.profile
    ) = AudioCodecConfig(mimeType, startBitrate, sampleRate, channelConfig, byteFormat, profile)

    override fun toString() =
        "AudioConfig(mimeType=$mimeType, startBitrate=$startBitrate, sampleRate=$sampleRate, channelConfig=$channelConfig, byteFormat=$byteFormat, profile=$profile)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioCodecConfig) return false

        if (!super.equals(other)) return false
        if (sampleRate != other.sampleRate) return false
        if (channelConfig != other.channelConfig) return false
        if (byteFormat != other.byteFormat) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelConfig
        result = 31 * result + byteFormat
        return result
    }


    companion object {
        private const val MIMETYPE_AAC_PREFIX = "audio/mp4a"

        private val opusSupportedSamplingRate = intArrayOf(
            8000,
            12000,
            16000,
            24000,
            48000
        )

        internal fun isAacMimeType(mimeType: String) = mimeType.startsWith(MIMETYPE_AAC_PREFIX)

        private fun getAacProfileFromMimeType(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> MediaCodecInfo.CodecProfileLevel.AACObjectLC
            else -> 0 // We suppose that profile is not needed when mimeType is an AAC extension
        }

        internal fun getDefaultSampleRate(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> 44_100
            MediaFormat.MIMETYPE_AUDIO_OPUS -> 48_000
            else -> throw InvalidParameterException("Mimetype not supported: $mimeType")
        }

        /**
         * Get the default audio profile
         */
        internal fun getDefaultProfile(mimeType: String) = when {
            isAacMimeType(mimeType) -> getAacProfileFromMimeType(mimeType)
            mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS -> 0
            else -> throw InvalidParameterException("Mimetype not supported: $mimeType")
        }

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

        /**
         * Returns the number of bytes for a single audio sample.
         *
         * @param byteFormat byte format (either [AudioFormat.ENCODING_PCM_8BIT] or [AudioFormat.ENCODING_PCM_16BIT],...)
         * @return number of bytes per sample
         */
        fun getNumOfBytesPerSample(byteFormat: Int) = when (byteFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT -> 4
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> throw InvalidParameterException("Byte format not supported: $byteFormat")
        }
    }
}
