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
package io.github.thibaultbee.streampack.internal.encoders

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Range
import io.github.thibaultbee.streampack.internal.utils.isAudio
import io.github.thibaultbee.streampack.internal.utils.isVideo
import java.security.InvalidParameterException

object MediaCodecHelper {
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

    /**
     * Get the default encoder that matches [format].
     *
     * @param format the media format
     * @return the encoder name
     */
    fun findEncoder(format: MediaFormat): String {
        return codecList.findEncoderForFormat(format)
            ?: throw InvalidParameterException("Failed to create codec for: $format")
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
     * Get the codec info for a particular encoder/decoder
     *
     * @param name the codec name
     * @return the media codec info
     */
    private fun getCodecInfo(name: String): MediaCodecInfo {
        return codecList.codecInfos.first { it.name == name }
    }

    object Video {
        /**
         * Get supported video encoders list
         */
        val supportedEncoders = getEncodersType { type -> type.isVideo() }

        /**
         * Get the name of all video encoders
         */
        val encodersName = getEncodersName { type -> type.isVideo() }

        /**
         * Get video encoder video capabilities for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the encoder video capabilities
         */
        private fun getCapabilities(mimeType: String): MediaCodecInfo.VideoCapabilities {
            require(mimeType.isVideo()) { "MimeType must be video" }

            val encoderName = findEncoder(mimeType)
            return getCapabilities(mimeType, encoderName)
        }

        /**
         * Get video encoder video capabilities for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the encoder video capabilities
         */
        private fun getCapabilities(
            mimeType: String,
            name: String
        ): MediaCodecInfo.VideoCapabilities {
            require(mimeType.isVideo()) { "MimeType must be video" }

            val codecInfo = getCodecInfo(name)
            return codecInfo.getCapabilitiesForType(
                mimeType
            ).videoCapabilities
        }

        /**
         * Get video encoder supported heights for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of supported heights
         */
        fun getSupportedHeights(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported heights for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of supported heights
         */
        fun getSupportedHeights(mimeType: String, name: String): Range<Int> =
            getCapabilities(mimeType, name).supportedHeights

        /**
         * Get video encoder supported widths for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of supported widths
         */
        fun getSupportedWidths(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported widths for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of supported widths
         */
        fun getSupportedWidths(mimeType: String, name: String): Range<Int> =
            getCapabilities(mimeType, name).supportedHeights

        /**
         * Get video encoder supported frame rate range for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of frame in b/s
         */
        fun getFramerateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedFrameRates

        /**
         * Get video encoder supported frame rate range for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of frame in b/s
         */
        fun getFramerateRange(mimeType: String, name: String): Range<Int> =
            getCapabilities(mimeType, name).supportedFrameRates

        /**
         * Get video encoder supported bitrate for the default video encoder.
         *
         * @param mimeType the video encoder mime type
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).bitrateRange

        /**
         * Get video encoder supported bitrate for the specified video encoder.
         *
         * @param mimeType the video encoder mime type
         * @param name the video encoder name
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String, name: String): Range<Int> =
            getCapabilities(mimeType, name).bitrateRange
    }

    object Audio {
        /**
         * Get supported audio encoders list
         */
        val supportedEncoders = getEncodersType { type -> type.isAudio() }


        /**
         * Get the name of all audio encoders
         */
        val encodersName = getEncodersName { type -> type.isAudio() }

        /**
         * Get encoder audio capabilities for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the encoder audio capabilities
         */
        private fun getCapabilities(mimeType: String): MediaCodecInfo.AudioCapabilities {
            require(mimeType.isAudio()) { "MimeType must be audio" }

            val encoderName = findEncoder(mimeType)
            return getCapabilities(mimeType, encoderName)
        }

        /**
         * Get encoder audio capabilities for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the encoder audio capabilities
         */
        private fun getCapabilities(
            mimeType: String,
            name: String
        ): MediaCodecInfo.AudioCapabilities {
            require(mimeType.isAudio()) { "MimeType must be audio" }

            val codecInfo = getCodecInfo(name)
            return codecInfo.getCapabilitiesForType(
                mimeType
            ).audioCapabilities
        }

        /**
         * Get maximum supported number of channel for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the maximum number of channel supported
         */
        fun getInputChannelRange(mimeType: String) =
            Range(1, getCapabilities(mimeType).maxInputChannelCount)

        /**
         * Get maximum supported number of channel for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the maximum number of channel supported
         */
        fun getInputChannelRange(mimeType: String, name: String) =
            Range(1, getCapabilities(mimeType, name).maxInputChannelCount)

        /**
         * Get audio encoder supported bitrate for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).bitrateRange

        /**
         * Get audio encoder supported bitrate for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String, name: String): Range<Int> =
            getCapabilities(mimeType, name).bitrateRange

        /**
         * Get audio encoder supported sample rates for the default audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @return the sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String): IntArray =
            getCapabilities(mimeType).supportedSampleRates

        /**
         * Get audio encoder supported sample rates for the specified audio encoder.
         *
         * @param mimeType the audio encoder mime type
         * @param name the audio encoder name
         * @return the sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String, name: String): IntArray =
            getCapabilities(mimeType, name).supportedSampleRates
    }
}