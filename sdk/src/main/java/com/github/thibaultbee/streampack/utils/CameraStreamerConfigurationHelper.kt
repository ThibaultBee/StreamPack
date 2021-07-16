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
package com.github.thibaultbee.streampack.utils

import android.content.Context
import android.util.Range
import android.util.Size
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer
import com.github.thibaultbee.streampack.internal.encoders.MediaCodecHelper
import com.github.thibaultbee.streampack.internal.muxers.ts.TSMuxerHelper
import com.github.thibaultbee.streampack.internal.sources.camera.getCameraFpsList
import com.github.thibaultbee.streampack.internal.sources.camera.getCameraOutputStreamSizes

/**
 * Configuration helper for [BaseCameraStreamer].
 * It filters supported values from MediaCodec, Camera and TS Muxer.
 */
object CameraStreamerConfigurationHelper {
    object Video {
        /**
         * Supported encoders for a [BaseCameraStreamer]
         */
        val supportedEncoders = TSMuxerHelper.Video.supportedEncoders.filter {
            MediaCodecHelper.Video.supportedEncoders.contains(it)
        }

        /**
         * Get supported resolutions for a [BaseCameraStreamer].
         *
         * @param context application context
         * @param mimeType video encoder mime type
         * @return list of resolutions
         */
        fun getSupportedResolutions(context: Context, mimeType: String): List<Size> {
            val codecSupportedWidths = MediaCodecHelper.Video.getSupportedWidths(mimeType)
            val codecSupportedHeights = MediaCodecHelper.Video.getSupportedHeights(mimeType)

            return context.getCameraOutputStreamSizes().filter {
                codecSupportedWidths.contains(it.width) && codecSupportedHeights.contains(it.height)
            }
        }

        /**
         * Get supported framerate for a [BaseCameraStreamer].
         *
         * @param context application context
         * @param mimeType video encoder mime type
         * @param cameraId camera id
         * @return list of framerates
         */
        fun getSupportedFramerates(
            context: Context,
            mimeType: String,
            cameraId: String
        ): List<Range<Int>> {
            val encoderFpsRange = MediaCodecHelper.Video.getFramerateRange(mimeType)
            return context.getCameraFpsList(cameraId).filter { encoderFpsRange.contains(it) }
        }

        /**
         * Get supported bitrate range for a [BaseCameraStreamer].
         *
         * @param mimeType video encoder mime type
         * @return bitrate range
         */
        fun getSupportedBitrates(mimeType: String) =
            MediaCodecHelper.Video.getBitrateRange(mimeType)
    }

    object Audio {
        /**
         * Get supported audio encoders list
         */
        val supportedEncoders = TSMuxerHelper.Audio.supportedEncoders.filter {
            MediaCodecHelper.Audio.supportedEncoders.contains(it)
        }

        /**
         * Get maximum supported number of channel by encoder.
         *
         * @param mimeType audio encoder mime type
         * @return maximum number of channel supported by the encoder
         */
        fun getSupportedInputChannelRange(mimeType: String) =
            MediaCodecHelper.Audio.getInputChannelRange(mimeType)

        /**
         * Get supported bitrate range for a [BaseCameraStreamer].
         *
         * @param mimeType audio encoder mime type
         * @return bitrate range
         */
        fun getSupportedBitrates(mimeType: String) =
            MediaCodecHelper.Audio.getBitrateRange(mimeType)

        /**
         * Get audio supported sample rates.
         *
         * @param mimeType audio encoder mime type
         * @return sample rates list in Hz.
         */
        fun getSupportedSampleRates(mimeType: String) =
            MediaCodecHelper.Audio.getSupportedSampleRates(mimeType)
    }
}