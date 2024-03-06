/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.streamers.helpers

import android.content.Context
import android.util.Range
import android.util.Size
import io.github.thibaultbee.streampack.internal.endpoints.CompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.FlvMuxerHelper
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.MP4MuxerHelper
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.TSMuxerHelper
import io.github.thibaultbee.streampack.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.utils.get10BitSupportedProfiles
import io.github.thibaultbee.streampack.utils.getCameraFpsList
import io.github.thibaultbee.streampack.utils.getCameraOutputStreamSizes

/**
 * Configuration helper for [BaseCameraStreamer].
 * It wraps supported values from MediaCodec, Camera and TS Muxer.
 */
class CameraStreamerConfigurationHelper(endpointHelper: IEndpoint.IEndpointHelper) :
    StreamerConfigurationHelper(endpointHelper) {
    companion object {
        val flvHelper = CameraStreamerConfigurationHelper(
            CompositeEndpoint.CompositeEndpointHelper(FlvMuxerHelper)
        )
        val tsHelper = CameraStreamerConfigurationHelper(
            CompositeEndpoint.CompositeEndpointHelper(TSMuxerHelper)
        )
        val mp4Helper = CameraStreamerConfigurationHelper(
            CompositeEndpoint.CompositeEndpointHelper(MP4MuxerHelper)
        )
    }

    override val video = VideoCameraStreamerConfigurationHelper(endpointHelper.video)
}

class VideoCameraStreamerConfigurationHelper(videoEndpointHelper: IEndpoint.IEndpointHelper.IVideoEndpointHelper) :
    VideoStreamerConfigurationHelper(videoEndpointHelper) {
    /**
     * Get camera resolutions that are supported by the encoder.
     *
     * @param context application context
     * @param mimeType video encoder mime type
     * @return list of resolutions
     */
    fun getSupportedResolutions(context: Context, mimeType: String): List<Size> {
        val pair = super.getSupportedResolutions(mimeType)
        val codecSupportedWidths = pair.first
        val codecSupportedHeights = pair.second

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
        val encoderFpsRange = super.getSupportedFramerate(mimeType)
        return context.getCameraFpsList(cameraId).filter { encoderFpsRange.contains(it) }
    }

    /**
     * Get supported 8-bit and 10-bit profiles for a [BaseCameraStreamer].
     *
     * @param context application context
     * @param mimeType video encoder mime type
     * @param cameraId camera id
     * @return list of profiles
     */
    fun getSupportedAllProfiles(
        context: Context,
        mimeType: String,
        cameraId: String
    ): List<Int> {
        val supportedDynamicRangeProfiles = context.get10BitSupportedProfiles(cameraId)

        // If device doesn't support 10-bit, return all supported 8-bit profiles
        return super.getSupportedAllProfiles(mimeType).filter {
            supportedDynamicRangeProfiles.contains(
                DynamicRangeProfile.fromProfile(
                    mimeType,
                    it
                ).dynamicRange
            )
        }
    }
}