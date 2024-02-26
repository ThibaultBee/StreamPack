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
package io.github.thibaultbee.streampack.internal.encoders.mediacodec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Range
import io.github.thibaultbee.streampack.internal.utils.extensions.isAudio
import io.github.thibaultbee.streampack.internal.utils.extensions.isVideo
import java.security.InvalidParameterException

object MediaCodecHelper {
    private val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

    /**
     * On Build.VERSION_CODES.LOLLIPOP, format must not contain a frame rate.
     * Use format.setString(MediaFormat.KEY_FRAME_RATE, null) to clear any existing frame
     * rate setting in the format.
     */
    private fun secureMediaFormatAction(
        format: MediaFormat,
        action: ((MediaFormat) -> Any?)
    ): Any? {
        val frameRate = try {
            format.getInteger(MediaFormat.KEY_FRAME_RATE)
        } catch (e: Exception) {
            null
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            format.setString(MediaFormat.KEY_FRAME_RATE, null)
        }
        val result = action(format)

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            frameRate?.let { format.setInteger(MediaFormat.KEY_FRAME_RATE, it) }
        }

        return result
    }

    /**
     * Get the default encoder that matches [format].
     *
     * @param format the media format
     * @return the encoder name
     */
    fun findEncoder(format: MediaFormat): String {
        val encoderName =
            secureMediaFormatAction(format) { codecList.findEncoderForFormat(format) } as String?
        return encoderName ?: throw InvalidParameterException("Failed to create codec for: $format")
    }

    /**
     * Get the default encoder for type [mimeType].
     *
     * @param mimeType the encoder mime type
     * @return the encoder name
     */
    fun findEncoder(mimeType: String): String {
        val format = MediaFormat().apply { setString(MediaFormat.KEY_MIME, mimeType) }
        return findEncoder(format)
    }

    /**
     * Get device encoder list.
     *
     * @param filter the filter a specific type of encoders
     * @return the list of supported encoder type
     */
    private fun getEncodersType(filter: ((String) -> Boolean)): List<String> {
        val encoders = mutableListOf<String>()
        codecList.codecInfos
            .filter { it.isEncoder }
            .flatMap { it.supportedTypes.toList() }
            .filter { filter(it) }
            .distinct()
            .forEach { encoders.add(it) }

        return encoders
    }

    /**
     * Get encoders name.
     *
     * @param filter the filter a specific type of encoders
     * @return the list of encoders name
     */
    private fun getEncodersName(filter: ((String) -> Boolean)): List<String> {
        val encoders = mutableListOf<String>()
        codecList.codecInfos
            .filter { it.isEncoder }
            .filter { it.supportedTypes.any { type -> filter(type) } }
            .distinct()
            .forEach { encoders.add(it.name) }

        return encoders
    }

    /**
     * Get the encoders for a particular mime type
     *
     * @param mimeType the video encoder mime type
     * @return the encoder name list
     * @see getTypesForName
     */
    fun getNamesForType(mimeType: String): List<String> {
        val encoders = mutableListOf<String>()
        codecList.codecInfos
            .filter { it.isEncoder }
            .filter { it.supportedTypes.any { type -> type == mimeType } }
            .distinct()
            .forEach { encoders.add(it.name) }

        return encoders
    }

    /**
     * Get the encoders for a particular mime type
     *
     * @param name the codec name
     * @return the encoder name list
     * @see getNamesForType
     */
    fun getTypesForName(name: String): List<String> {
        return getCodecInfo(name).supportedTypes.toList()
    }

    /**
     * Get the codec info for a particular encoder/decoder
     *
     * @param name the codec name
     * @return the media codec info
     */
    fun getCodecInfo(name: String): MediaCodecInfo = codecList.codecInfos.first { it.name == name }

    /**
     * Check if codec is hardware accelerated
     *
     * @param codecInfo the codec info
     * @return trie if the codec is hardware accelerated
     */
    fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return codecInfo.isHardwareAccelerated
        }

        return !isSoftwareOnly(codecInfo)
    }

    // From Exoplayer https://github.com/google/ExoPlayer/blob/dev-v2/library/transformer/src/main/java/com/google/android/exoplayer2/transformer/EncoderUtil.java
    fun isSoftwareOnly(codecInfo: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return codecInfo.isSoftwareOnly
        }

        val codecName: String = codecInfo.name.lowercase()
        return if (codecName.startsWith("arc.")) {
            // App Runtime for Chrome (ARC) codecs
            false
        } else codecName.startsWith("omx.google.")
                || codecName.startsWith("omx.ffmpeg.")
                || codecName.startsWith("omx.sec.") && codecName.contains(".sw.")
                || codecName == "omx.qcom.video.decoder.hevcswvdec" || codecName.startsWith("c2.android.")
                || codecName.startsWith("c2.google.")
                || !codecName.startsWith("omx.") && !codecName.startsWith("c2.")
    }

    /**
     * Get encoder capabilities for the default encoder.
     *
     * @param mimeType the encoder mime type
     * @return the encoder capabilities
     */
    private fun getCodecCapabilities(mimeType: String): MediaCodecInfo.CodecCapabilities {
        val encoderName = findEncoder(mimeType)
        return getCodecCapabilities(mimeType, encoderName)
    }

    /**
     * Get encoder capabilities for the specified encoder.
     *
     * @param mimeType the encoder mime type
     * @param name the encoder name
     * @return the encoder capabilities
     */
    private fun getCodecCapabilities(
        mimeType: String,
        name: String
    ): MediaCodecInfo.CodecCapabilities {
        val codecInfo = getCodecInfo(name)
        return codecInfo.getCapabilitiesForType(
            mimeType
        )
    }

    /**
     * Get encoder supported profile level list for the default encoder.
     *
     * @param mimeType the encoder mime type
     * @return the profile level list
     */
    fun getProfileLevel(
        mimeType: String,
    ): List<MediaCodecInfo.CodecProfileLevel> =
        getCodecCapabilities(mimeType).profileLevels.toList().toSet().toList()

    /**
     * Get encoder supported profile level list for the specified encoder.
     *
     * @param mimeType the encoder mime type
     * @param name the encoder name
     * @return the profile level list
     */
    fun getProfileLevel(
        mimeType: String,
        name: String
    ): List<MediaCodecInfo.CodecProfileLevel> =
        getCodecCapabilities(mimeType, name).profileLevels.toList().toSet().toList()

    /**
     * Get encoder supported profiles list for the default encoder.
     *
     * @param mimeType the encoder mime type
     * @return the profiles list
     */
    fun getProfiles(
        mimeType: String,
    ): List<Int> =
        getProfileLevel(mimeType).map { it.profile }.toSet().toList()

    /**
     * Get encoder supported profiles list for the specified encoder.
     *
     * @param mimeType the encoder mime type
     * @param name the encoder name
     * @return the profiles list
     */
    fun getProfiles(
        mimeType: String,
        name: String
    ): List<Int> =
        getProfileLevel(mimeType, name).map { it.profile }.toSet().toList()

    /**
     * Get encoder maximum supported levels for the default encoder.
     *
     * @param mimeType the encoder mime type
     * @param profile the encoder profile
     * @return the maximum level
     */
    fun getMaxLevel(
        mimeType: String,
        profile: Int
    ): Int {
        return getProfileLevel(mimeType)
            .filter { it.profile == profile }
            .maxOf { it.level }
    }

    /**
     * Get encoder maximum supported levels for the the specified encoder.
     *
     * @param mimeType the encoder mime type
     * @param name the encoder name
     * @param profile the encoder profile
     * @return the maximum level
     */
    fun getMaxLevel(
        mimeType: String,
        name: String,
        profile: Int
    ): Int {
        return getProfileLevel(mimeType, name)
            .filter { it.profile == profile }
            .maxOf { it.level }
    }

    /**
     * Check if format is supported by default encoder.
     *
     * @param format the media format to check
     * @return true if format is supported, otherwise false
     */
    fun isFormatSupported(format: MediaFormat): Boolean {
        val mimeType = format.getString(MediaFormat.KEY_MIME) as String
        return secureMediaFormatAction(format) {
            getCodecCapabilities(mimeType).isFormatSupported(
                format
            )
        } as Boolean
    }

    /**
     * Check if format is supported by default encoder.
     *
     * @param format the media format to check
     * @param name the encoder name
     * @return true if format is supported, otherwise false
     */
    fun isFormatSupported(format: MediaFormat, name: String): Boolean {
        val mimeType = format.getString(MediaFormat.KEY_MIME) as String
        return secureMediaFormatAction(format) {
            getCodecCapabilities(mimeType, name).isFormatSupported(
                format
            )
        } as Boolean
    }

    /**
     * Whether the encoder supports the specified feature.
     *
     * @param mimeType the encoder mime type
     * @param feature the feature to check
     * @return true if the feature is supported, otherwise false
     * @see MediaCodecInfo.CodecCapabilities.isFeatureSupported
     */
    fun isFeatureSupported(
        mimeType: String,
        feature: String
    ) = getCodecCapabilities(mimeType).isFeatureSupported(feature)

    /**
     * Whether the encoder supports the specified feature.
     *
     * @param mimeType the encoder mime type
     * @param name the encoder name
     * @param feature the feature to check
     * @return true if the feature is supported, otherwise false
     * @see MediaCodecInfo.CodecCapabilities.isFeatureSupported
     */
    fun isFeatureSupported(
        mimeType: String,
        name: String,
        feature: String
    ) = getCodecCapabilities(mimeType, name).isFeatureSupported(feature)

    object Video {
        /**
         * Get supported video encoders list
         */
        val supportedEncoders = getEncodersType { type -> type.isVideo }

        /**
         * Get the name of all video encoders
         */
        val encodersName = getEncodersName { type -> type.isVideo }

        /**
         * Get video encoder video capabilities for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the encoder video capabilities
         */
        private fun getVideoCapabilities(mimeType: String): MediaCodecInfo.VideoCapabilities {
            require(mimeType.isVideo) { "MimeType must be video" }

            val encoderName = findEncoder(mimeType)
            return getVideoCapabilities(mimeType, encoderName)
        }

        /**
         * Get video encoder video capabilities for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the encoder video capabilities
         */
        private fun getVideoCapabilities(
            mimeType: String,
            name: String
        ): MediaCodecInfo.VideoCapabilities {
            require(mimeType.isVideo) { "MimeType must be video" }

            return getCodecCapabilities(mimeType, name).videoCapabilities
        }

        /**
         * Get video encoder supported heights for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of supported heights
         */
        fun getSupportedHeights(mimeType: String): Range<Int> =
            getVideoCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported heights for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of supported heights
         */
        fun getSupportedHeights(mimeType: String, name: String): Range<Int> =
            getVideoCapabilities(mimeType, name).supportedHeights

        /**
         * Get video encoder supported widths for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of supported widths
         */
        fun getSupportedWidths(mimeType: String): Range<Int> =
            getVideoCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported widths for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of supported widths
         */
        fun getSupportedWidths(mimeType: String, name: String): Range<Int> =
            getVideoCapabilities(mimeType, name).supportedHeights

        /**
         * Get video encoder supported frame rate range for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of frame in b/s
         */
        fun getFramerateRange(mimeType: String): Range<Int> =
            getVideoCapabilities(mimeType).supportedFrameRates

        /**
         * Get video encoder supported frame rate range for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of frame in b/s
         */
        fun getFramerateRange(mimeType: String, name: String): Range<Int> =
            getVideoCapabilities(mimeType, name).supportedFrameRates

        /**
         * Get video encoder supported bitrate for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getVideoCapabilities(mimeType).bitrateRange

        /**
         * Get video encoder supported bitrate for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String, name: String): Range<Int> =
            getVideoCapabilities(mimeType, name).bitrateRange
    }

    object Audio {
        /**
         * Get supported audio encoders list
         */
        val supportedEncoders = getEncodersType { type -> type.isAudio }


        /**
         * Get the name of all audio encoders
         */
        val encodersName = getEncodersName { type -> type.isAudio }

        /**
         * Get encoder audio capabilities for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the encoder audio capabilities
         */
        private fun getAudioCapabilities(mimeType: String): MediaCodecInfo.AudioCapabilities {
            require(mimeType.isAudio) { "MimeType must be audio" }

            val encoderName = findEncoder(mimeType)
            return getAudioCapabilities(mimeType, encoderName)
        }

        /**
         * Get encoder audio capabilities for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the encoder audio capabilities
         */
        private fun getAudioCapabilities(
            mimeType: String,
            name: String
        ): MediaCodecInfo.AudioCapabilities {
            require(mimeType.isAudio) { "MimeType must be audio" }

            return getCodecCapabilities(mimeType, name).audioCapabilities
        }

        /**
         * Get maximum supported number of channel for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the maximum number of channel supported
         */
        fun getInputChannelRange(mimeType: String) =
            Range(1, getAudioCapabilities(mimeType).maxInputChannelCount)

        /**
         * Get maximum supported number of channel for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the maximum number of channel supported
         */
        fun getInputChannelRange(mimeType: String, name: String) =
            Range(1, getAudioCapabilities(mimeType, name).maxInputChannelCount)

        /**
         * Get audio encoder supported bitrate for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getAudioCapabilities(mimeType).bitrateRange

        /**
         * Get audio encoder supported bitrate for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String, name: String): Range<Int> =
            getAudioCapabilities(mimeType, name).bitrateRange

        /**
         * Get audio encoder supported sample rates for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String): IntArray =
            getAudioCapabilities(mimeType).supportedSampleRates

        /**
         * Get audio encoder supported sample rates for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String, name: String): IntArray =
            getAudioCapabilities(mimeType, name).supportedSampleRates
    }
}