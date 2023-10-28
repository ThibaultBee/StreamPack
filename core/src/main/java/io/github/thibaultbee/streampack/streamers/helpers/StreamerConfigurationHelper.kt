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
import android.media.MediaCodecInfo.CodecProfileLevel.*
import android.media.MediaFormat
import android.util.Range
import io.github.thibaultbee.streampack.internal.encoders.MediaCodecHelper
import io.github.thibaultbee.streampack.internal.muxers.IAudioMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.IMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.IVideoMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.mp4.MP4MuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxerHelper
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import java.security.InvalidParameterException

/**
 * Configuration helper for [BaseStreamer].
 * It wraps supported values from MediaCodec and TS Muxer.
 *
 * @param muxerHelper the corresponding muxer helper
 */
open class StreamerConfigurationHelper(muxerHelper: IMuxerHelper) :
    IConfigurationHelper {
    companion object {
        val flvHelper = StreamerConfigurationHelper(FlvMuxerHelper())
        val tsHelper = StreamerConfigurationHelper(TSMuxerHelper())
        val mp4Helper = StreamerConfigurationHelper(MP4MuxerHelper())
    }

    override val audio =
        AudioStreamerConfigurationHelper(muxerHelper.audio)
    override val video =
        VideoStreamerConfigurationHelper(muxerHelper.video)
}

class AudioStreamerConfigurationHelper(private val audioMuxerHelper: IAudioMuxerHelper) :
    IAudioConfigurationHelper {
    /**
     * Get supported audio encoders list
     */
    override val supportedEncoders = MediaCodecHelper.Audio.supportedEncoders.filter {
        audioMuxerHelper.supportedEncoders.contains(it)
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
        return audioMuxerHelper.getSupportedSampleRates()?.let { muxerSampleRates ->
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
        return audioMuxerHelper.getSupportedByteFormats()?.let { muxerByteFormats ->
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

open class VideoStreamerConfigurationHelper(private val videoMuxerHelper: IVideoMuxerHelper) :
    IVideoConfigurationHelper {
    /**
     * Supported encoders for a [BaseStreamer]
     */
    override val supportedEncoders = MediaCodecHelper.Video.supportedEncoders.filter {
        videoMuxerHelper.supportedEncoders.contains(it)
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
     * Get supported profiles for a [BaseStreamer].
     * Removes profiles for 10 bits and still images.
     *
     * @param mimeType video encoder mime type
     * @return list of profile
     */
    fun getSupportedProfiles(mimeType: String): List<Int> {
        val profiles = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfiles
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfiles
            MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9Profiles
            MediaFormat.MIMETYPE_VIDEO_AV1 -> av1Profiles
            else -> throw InvalidParameterException("Unknown mimetype $mimeType")
        }
        val supportedProfiles = MediaCodecHelper.getProfiles(mimeType)
        return supportedProfiles.filter { profiles.contains(it) }
    }

    private val avcProfiles = listOf(
        AVCProfileBaseline,
        AVCProfileConstrainedBaseline,
        AVCProfileConstrainedHigh,
        AVCProfileExtended,
        AVCProfileHigh,
        AVCProfileMain
    )

    private val hevcProfiles = listOf(
        HEVCProfileMain
    )

    private val vp9Profiles = listOf(
        VP9Profile0,
        VP9Profile1
    )

    private val av1Profiles = listOf(
        AV1ProfileMain8
    )
}

