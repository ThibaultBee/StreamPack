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

import android.media.AudioFormat
import android.media.MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileExtended
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus
import android.media.MediaFormat
import android.os.Build
import android.util.Range
import io.github.thibaultbee.streampack.internal.encoders.MediaCodecHelper
import io.github.thibaultbee.streampack.internal.endpoints.CompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.FlvMuxerHelper
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.MP4MuxerHelper
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.TSMuxerHelper
import io.github.thibaultbee.streampack.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import java.security.InvalidParameterException

/**
 * Configuration helper for [BaseStreamer].
 * It wraps supported values from MediaCodec and TS Muxer.
 *
 * @param endpointHelper the corresponding muxer helper
 */
open class StreamerConfigurationHelper(endpointHelper: IEndpoint.IEndpointHelper) :
    IConfigurationHelper {
    companion object {
        val flvHelper =
            StreamerConfigurationHelper(CompositeEndpoint.CompositeEndpointHelper(FlvMuxerHelper))
        val tsHelper =
            StreamerConfigurationHelper(CompositeEndpoint.CompositeEndpointHelper(TSMuxerHelper))
        val mp4Helper =
            StreamerConfigurationHelper(CompositeEndpoint.CompositeEndpointHelper(MP4MuxerHelper))
    }

    override val audio =
        AudioStreamerConfigurationHelper(endpointHelper.audio)
    override val video =
        VideoStreamerConfigurationHelper(endpointHelper.video)
}

class AudioStreamerConfigurationHelper(private val audioEndpointHelper: IEndpoint.IEndpointHelper.IAudioEndpointHelper) :
    IAudioConfigurationHelper {
    /**
     * Get supported audio encoders list
     */
    override val supportedEncoders = MediaCodecHelper.Audio.supportedEncoders.filter {
        audioEndpointHelper.supportedEncoders.contains(it)
    }

    /**
     * Get supported bitrate range for a [BaseStreamer].
     *
     * @param mimeType audio encoder mime type
     * @return bitrate range
     */
    override fun getSupportedBitrates(mimeType: String) =
        MediaCodecHelper.Audio.getBitrateRange(mimeType)

    /**
     * Get maximum supported number of channel by encoder.
     *
     * @param mimeType audio encoder mime type
     * @return maximum number of channel supported by the encoder
     */
    override fun getSupportedInputChannelRange(mimeType: String) =
        MediaCodecHelper.Audio.getInputChannelRange(mimeType)

    /**
     * Get audio supported sample rates.
     *
     * @param mimeType audio encoder mime type
     * @return sample rates list in Hz.
     */
    override fun getSupportedSampleRates(mimeType: String): List<Int> {
        val codecSampleRates = MediaCodecHelper.Audio.getSupportedSampleRates(mimeType).toList()
        return audioEndpointHelper.supportedSampleRates?.let { muxerSampleRates ->
            codecSampleRates.filter {
                muxerSampleRates.contains(it)
            }
        } ?: codecSampleRates
    }

    /**
     * Get audio supported byte formats.
     *
     * @return audio byte format.
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    override fun getSupportedByteFormats(): List<Int> {
        val codecByteFormats = listOf(
            AudioFormat.ENCODING_PCM_8BIT,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        return audioEndpointHelper.supportedByteFormats?.let { muxerByteFormats ->
            codecByteFormats.filter {
                muxerByteFormats.contains(it)
            }
        } ?: codecByteFormats
    }

    /**
     * Get supported profiles for a [BaseStreamer].
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    fun getSupportedProfiles(mimeType: String): List<Int> {
        return MediaCodecHelper.getProfiles(mimeType)
    }
}

open class VideoStreamerConfigurationHelper(private val videoEndpointHelper: IEndpoint.IEndpointHelper.IVideoEndpointHelper) :
    IVideoConfigurationHelper {
    /**
     * Supported encoders for a [BaseStreamer]
     */
    override val supportedEncoders = MediaCodecHelper.Video.supportedEncoders.filter {
        videoEndpointHelper.supportedEncoders.contains(it)
    }

    /**
     * Get supported bitrate range for a [BaseStreamer].
     *
     * @param mimeType video encoder mime type
     * @return bitrate range
     */
    override fun getSupportedBitrates(mimeType: String) =
        MediaCodecHelper.Video.getBitrateRange(mimeType)

    /**
     * Get supported resolutions range for then encoder.
     *
     * @param mimeType video encoder mime type
     * @return pair that contains supported width ([Pair.first]) and supported height ([Pair.second]).
     */
    open fun getSupportedResolutions(mimeType: String): Pair<Range<Int>, Range<Int>> {
        val codecSupportedWidths = MediaCodecHelper.Video.getSupportedWidths(mimeType)
        val codecSupportedHeights = MediaCodecHelper.Video.getSupportedHeights(mimeType)

        return Pair(codecSupportedWidths, codecSupportedHeights)
    }

    /**
     * Get supported framerate for a [BaseStreamer].
     *
     * @param mimeType video encoder mime type
     * @return framerate range supported by encoder
     */
    fun getSupportedFramerate(
        mimeType: String,
    ) = MediaCodecHelper.Video.getFramerateRange(mimeType)

    /**
     * Get supported 8-bit and 10-bit profiles for a [BaseStreamer].
     * Removes profiles for 10 bits and still images.
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    fun getSupportedAllProfiles(mimeType: String): List<Int> {
        val profiles = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfiles
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfiles
            MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9Profiles
            MediaFormat.MIMETYPE_VIDEO_AV1 -> av1Profiles
            else -> throw InvalidParameterException("Unknown mimetype $mimeType")
        }
        val supportedProfiles = MediaCodecHelper.getProfiles(mimeType)
        return profiles.filter {
            supportedProfiles.contains(it) &&
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (DynamicRangeProfile.fromProfile(
                                mimeType,
                                it
                            ).isHdr
                        ) {
                            MediaCodecHelper.isFeatureSupported(mimeType, FEATURE_HdrEditing)
                        } else {
                            true
                        }
                    } else {
                        true
                    }
        }
    }

    /**
     * Get supported HDR (10-bit only) profiles for a [BaseStreamer].
     * Removes profiles for 8 bits and still images.
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    fun getSupportedHdrProfiles(mimeType: String): List<Int> {
        val supportedProfiles = getSupportedAllProfiles(mimeType)
        return supportedProfiles.filter {
            DynamicRangeProfile.fromProfile(
                mimeType,
                it
            ).isHdr
        }
    }

    /**
     * Get supported SDR (8-bit only) profiles for a [BaseStreamer].
     * Removes profiles for 10 bits and still images.
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    @Deprecated(
        "Use [getSdrProfilesSupported] instead",
        ReplaceWith("getSdrProfilesSupported(mimeType)")
    )
    fun getSupportedProfiles(mimeType: String) = getSupportedSdrProfiles(mimeType)

    /**
     * Get supported SDR (8-bit only) profiles for a [BaseStreamer].
     * Removes profiles for 10 bits and still images.
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    fun getSupportedSdrProfiles(mimeType: String): List<Int> {
        val supportedProfiles = getSupportedAllProfiles(mimeType)
        return supportedProfiles.filter {
            !DynamicRangeProfile.fromProfile(
                mimeType,
                it
            ).isHdr
        }
    }

    /**
     * Only 420 profiles (8 and 10 bits) are supported.
     */
    private val avcProfiles = listOf(
        AVCProfileBaseline,
        AVCProfileConstrainedBaseline,
        AVCProfileConstrainedHigh,
        AVCProfileExtended,
        AVCProfileHigh,
        AVCProfileHigh10,
        AVCProfileMain,
    )

    private val hevcProfiles = listOf(
        HEVCProfileMain,
        HEVCProfileMain10,
        HEVCProfileMain10HDR10,
        HEVCProfileMain10HDR10Plus
    )

    private val vp9Profiles = listOf(
        VP9Profile0,
        VP9Profile2,
        VP9Profile2HDR,
        VP9Profile2HDR10Plus
    )

    private val av1Profiles = listOf(
        AV1ProfileMain8,
        AV1ProfileMain10,
        AV1ProfileMain10HDR10,
        AV1ProfileMain10HDR10Plus
    )
}

