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
package com.github.thibaultbee.streampack.internal.encoders

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Range
import com.github.thibaultbee.streampack.utils.isAudio
import com.github.thibaultbee.streampack.utils.isVideo

object MediaCodecHelper {
    object Video {
        /**
         * Get supported video encoders list
         */
        val supportedEncoders = getSupportedMediaCodecEncoders()

        /**
         * Get device video encoder list.
         *
         * @return list of supported video encoder
         */
        private fun getSupportedMediaCodecEncoders(): List<String> {
            val encoders = mutableListOf<String>()
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { it.isEncoder }
                .flatMap { it.supportedTypes.toList() }
                .filter { it.isVideo() }
                .forEach { encoders.add(it) }

            return encoders
        }

        /**
         * Get video encoder video capabilities.
         *
         * @param mimeType video encoder mime type
         * @return encoder video capabilities
         */
        private fun getCapabilities(mimeType: String): MediaCodecInfo.VideoCapabilities {
            require(mimeType.isVideo()) { "Mime Type must be video" }

            val format = MediaFormat().apply { setString(MediaFormat.KEY_MIME, mimeType) }
            val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderName = mediaCodecList.findEncoderForFormat(format)

            val mediaCodec = MediaCodec.createByCodecName(encoderName)
            val videoCapabilities = mediaCodec.codecInfo.getCapabilitiesForType(
                mimeType
            ).videoCapabilities
            mediaCodec.release()
            return videoCapabilities
        }

        /**
         * Get video encoder supported heights.
         *
         * @param mimeType video encoder mime type
         * @return range of supported heights
         */
        fun getSupportedHeights(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported widths.
         *
         * @param mimeType video encoder mime type
         * @return range of supported widths
         */
        fun getSupportedWidths(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedHeights

        /**
         * Get video encoder supported frame rate range.
         *
         * @param mimeType video encoder mime type
         * @return range of frame in b/s
         */
        fun getFramerateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).supportedFrameRates

        /**
         * Get video encoder supported bitrate.
         *
         * @param mimeType video encoder mime type
         * @return range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).bitrateRange
    }

    object Audio {
        /**
         * Get supported video encoders list
         */
        val supportedEncoders = getSupportedMediaCodecEncoders()

        /**
         * Get device audio encoder list.
         *
         * @return list of supported audio encoder
         */
        private fun getSupportedMediaCodecEncoders(): List<String> {
            val encoders = mutableListOf<String>()
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .filter { it.isEncoder }
                .flatMap { it.supportedTypes.toList() }
                .filter { it.isAudio() }
                .forEach { encoders.add(it) }

            return encoders
        }

        /**
         * Get encoder audio capabilities.
         *
         * @param mimeType audio encoder mime type
         * @return encoder audio capabilities
         */
        private fun getCapabilities(mimeType: String): MediaCodecInfo.AudioCapabilities {
            require(mimeType.isAudio()) { "Mime Type must be audio" }

            val format = MediaFormat().apply { setString(MediaFormat.KEY_MIME, mimeType) }
            val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderName = mediaCodecList.findEncoderForFormat(format)

            val mediaCodec = MediaCodec.createByCodecName(encoderName)
            val audioCapabilities = mediaCodec.codecInfo.getCapabilitiesForType(
                mimeType
            ).audioCapabilities
            mediaCodec.release()
            return audioCapabilities
        }

        /**
         * Get maximum supported number of channel by the audio encoder.
         *
         * @param mimeType audio encoder mime type
         * @return maximum number of channel supported
         */
        fun getInputChannelRange(mimeType: String) =
            Range(1, getCapabilities(mimeType).maxInputChannelCount)

        /**
         * Get audio encoder supported bitrate.
         *
         * @param mimeType audio encoder mime type
         * @return range of bitrate in b/s
         */
        fun getBitrateRange(mimeType: String): Range<Int> =
            getCapabilities(mimeType).bitrateRange

        /**
         * Get audio encoder supported sample rates.
         *
         * @param mimeType audio encoder mime type
         * @return sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String) =
            getCapabilities(mimeType).supportedSampleRates
    }
}