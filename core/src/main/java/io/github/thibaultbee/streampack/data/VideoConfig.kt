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
package io.github.thibaultbee.streampack.data

import android.content.Context
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileExtended
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile1
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import io.github.thibaultbee.streampack.internal.encoders.MediaCodecHelper
import io.github.thibaultbee.streampack.internal.utils.extensions.deviceOrientation
import io.github.thibaultbee.streampack.internal.utils.extensions.isVideo
import io.github.thibaultbee.streampack.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.utils.OrientationUtils
import java.security.InvalidParameterException
import kotlin.math.roundToInt

/**
 * Video configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [BaseStreamer.configure]
 */
class VideoConfig(
    /**
     * Video encoder mime type.
     * Only [MediaFormat.MIMETYPE_VIDEO_AVC], [MediaFormat.MIMETYPE_VIDEO_HEVC] and
     * [MediaFormat.MIMETYPE_VIDEO_VP9] are supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    /**
     * Video encoder bitrate in bits/s.
     */
    startBitrate: Int = 2000000,
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size = Size(1280, 720),
    /**
     * Video framerate.
     * This is a best effort as few camera can not generate a fixed framerate.
     */
    val fps: Int = 30,
    /**
     * Video encoder profile. Encoders may not support requested profile. In this case, StreamPack fallbacks to default profile.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    profile: Int = getBestProfile(mimeType),
    /**
     * Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to default level.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val level: Int = getBestLevel(mimeType, profile),
    /**
     * Video encoder I-frame interval in seconds.
     * This is a best effort as few camera can not generate a fixed framerate.
     * For live streaming, I-frame interval should be really low. For recording, I-frame interval should be higher.
     * A value of 0 means that each frame is an I-frame.
     * On device with API < 25, this value will be rounded to an integer. So don't expect a precise value and any value < 0.5 will be considered as 0.
     */
    val gopDuration: Float = 1f  // 1s between I frames
) : Config(mimeType, startBitrate, profile) {
    init {
        require(mimeType.isVideo) { "MimeType must be video" }
    }

    constructor(
        /**
         * Video encoder mime type.
         * Only [MediaFormat.MIMETYPE_VIDEO_AVC], [MediaFormat.MIMETYPE_VIDEO_HEVC] and [MediaFormat.MIMETYPE_VIDEO_VP9] are supported yet.
         *
         * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
         */
        mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        /**
         * Video output resolution in pixel.
         */
        resolution: Size = Size(1280, 720),
        /**
         * Video encoder bitrate in bits/s.
         */
        startBitrate: Int = getBestBitrate(resolution),
        /**
         * Video framerate.
         * This is a best effort as few camera can not generate a fixed framerate.
         */
        fps: Int = 30,
        /**
         * Video encoder profile/level. Encoders may not support requested profile. In this case, StreamPack fallbacks to default profile.
         * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
         */
        profileLevel: CodecProfileLevel,
        /**
         * Video encoder I-frame interval in seconds.
         * This is a best effort as few camera can not generate a fixed framerate.
         * For live streaming, I-frame interval should be really low. For recording, I-frame interval should be higher.
         */
        gopDuration: Float = 1f  // 1s between I frames
    ) : this(
        mimeType,
        startBitrate,
        resolution,
        fps,
        profileLevel.profile,
        profileLevel.level,
        gopDuration
    )

    /**
     * Get resolution according to device orientation
     *
     * @param context activity context
     * @return oriented resolution
     */
    fun getDeviceOrientedResolution(context: Context) =
        getOrientedResolution(context.deviceOrientation)

    /**
     * Get resolution according to orientation provided
     *
     * @param orientation the orientation
     * @return oriented resolution
     */
    fun getOrientedResolution(orientation: Int): Size {
        return if (OrientationUtils.isPortrait(orientation)) {
            resolution.portraitize()
        } else {
            resolution.landscapize()
        }
    }

    /**
     * Get the media format from the video configuration
     *
     * @return the corresponding video media format
     */
    override fun getFormat(withProfileLevel: Boolean): MediaFormat {
        val format = MediaFormat.createVideoFormat(
            mimeType,
            resolution.width,
            resolution.height
        )

        // Extended video format
        format.setInteger(MediaFormat.KEY_BIT_RATE, startBitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, gopDuration)
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, gopDuration.roundToInt())
        }

        if (withProfileLevel) {
            format.setInteger(MediaFormat.KEY_PROFILE, profile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                format.setInteger(MediaFormat.KEY_LEVEL, level)
            }
        }

        return format
    }

    companion object {
        /**
         * Return the best bitrate according to resolution
         *
         * @param resolution the resolution
         * @return the corresponding bitrate
         */
        fun getBestBitrate(resolution: Size): Int {
            val numOfPixels = resolution.width * resolution.height
            return when {
                numOfPixels <= 320 * 240 -> 800000
                numOfPixels <= 640 * 480 -> 1000000
                numOfPixels <= 1280 * 720 -> 2000000
                numOfPixels <= 1920 * 1080 -> 3500000
                else -> 4000000
            }
        }

        // Higher priority first
        private val avcProfilePriority = listOf(
            AVCProfileHigh,
            AVCProfileMain,
            AVCProfileExtended,
            AVCProfileBaseline,
            AVCProfileConstrainedHigh,
            AVCProfileConstrainedBaseline
        )

        private val hevcProfilePriority = listOf(
            HEVCProfileMain
        )

        private val vp9ProfilePriority = listOf(
            VP9Profile1,
            VP9Profile0
        )

        /**
         * Return the higher profile with the higher level
         */
        fun getBestProfile(mimeType: String): Int {
            val profilePriority = when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfilePriority
                MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfilePriority
                MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9ProfilePriority
                else -> throw InvalidParameterException("Profile for $mimeType is not supported")
            }

            val profileLevelList = MediaCodecHelper.getProfileLevel(mimeType)
            for (profile in profilePriority) {
                if (profileLevelList.any { it.profile == profile }) {
                    return profile
                }
            }

            throw UnsupportedOperationException("Failed to find a profile for $mimeType")
        }

        fun getBestLevel(mimeType: String, profile: Int) =
            MediaCodecHelper.getMaxLevel(mimeType, profile)
    }

    override fun toString() =
        "VideoConfig(mimeType='$mimeType', startBitrate=$startBitrate, resolution=$resolution, fps=$fps, profile=$profile, level=$level)"
}

