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

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel.APVProfile422_10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileExtended
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.H263ProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.VP8ProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaFormat
import android.media.MediaFormat.KEY_PRIORITY
import android.os.Build
import android.util.Size
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.getBestLevel
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.getBestProfile
import io.github.thibaultbee.streampack.core.elements.encoders.mediacodec.MediaCodecHelper
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.SdrColorStandardValue
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isVideo
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotateFromNaturalOrientation
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotationToDegrees
import java.security.InvalidParameterException
import kotlin.math.roundToInt

/**
 * Video configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 */
class VideoCodecConfig internal constructor(
    /**
     * Video encoder mime type.
     * Only [MediaFormat.MIMETYPE_VIDEO_AVC], [MediaFormat.MIMETYPE_VIDEO_HEVC],
     * [MediaFormat.MIMETYPE_VIDEO_VP9] and [MediaFormat.MIMETYPE_VIDEO_AV1] are supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    /**
     * Video encoder bitrate in bits/s.
     */
    startBitrate: Int = 2_000_000,
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size = DEFAULT_RESOLUTION,
    /**
     * Video framerate.
     * This is a best effort as few camera can not generate a fixed framerate.
     */
    val fps: Int = DEFAULT_FPS,
    /**
     * Video encoder I-frame interval in seconds.
     * This is a best effort as few camera can not generate a fixed frame rate.
     * For live streaming, I-frame interval should be really low. For recording, I-frame interval should be higher.
     * A value of 0 means that each frame is an I-frame.
     * On device with API < 25, this value will be rounded to an integer. So don't expect a precise value and any value < 0.5 will be considered as 0.
     */
    val gopDurationInS: Float = 1f,  // 1s between I frames
    /**
     * Video profile level with color information.
     */
    private val profileLevelColor: VideoProfileLevelColor,
    /**
     * A callback to be invoked when the media format is generated.
     * This is a dangerous callback as a wrong media format can make some encoders fail, also
     * don't change existing keys as it can break your streaming.
     * Also, don't block the thread.
     */
    private val customize: MediaFormatCustomHandler = {}
) : CodecConfig(mimeType, startBitrate, profileLevelColor.profile) {
    /**
     * Instantiates a [VideoCodecConfig] instance from profile and level.
     *
     * If you don't know how to set profile and level, use [VideoCodecConfig] without profile and level parameters.
     *
     * @param mimeType Video encoder mime type.
     * @param startBitrate Video encoder bitrate in bits/s.
     * @param resolution Video output resolution in pixel.
     * @param fps Video framerate.
     * @param gopDurationInS Video encoder I-frame interval in seconds.
     * @param profile Video encoder profile. Encoders may not support requested profile. In this case, StreamPack fallbacks to encoder default profile.
     * @param level Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to encoder default level.
     * @param customize A callback to be invoked when the media format is generated.
     */
    constructor(
        mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        startBitrate: Int = 2_000_000,
        resolution: Size = DEFAULT_RESOLUTION,
        fps: Int = DEFAULT_FPS,
        gopDurationInS: Float = 1f,  // 1s between I frames
        profile: Int,
        level: Int = getBestLevel(mimeType, profile),
        customize: MediaFormatCustomHandler = {}
    ) : this(
        mimeType,
        startBitrate,
        resolution,
        fps,
        gopDurationInS,
        {
            this.profile = profile
            this.level = level
        },
        customize
    )

    /**
     * Instantiates a [VideoCodecConfig] instance from [VideoProfileLevelColor.Builder].
     *
     * @param mimeType Video encoder mime type.
     * @param startBitrate Video encoder bitrate in bits/s.
     * @param resolution Video output resolution in pixel.
     * @param fps Video framerate.
     * @param gopDurationInS Video encoder I-frame interval in seconds.
     * @param profileLevelColorBuilder A builder to create [VideoProfileLevelColor].
     * @param customize A callback to be invoked when the media format is generated.
     */
    constructor(
        mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        startBitrate: Int = 2_000_000,
        resolution: Size = DEFAULT_RESOLUTION,
        fps: Int = DEFAULT_FPS,
        gopDurationInS: Float = 1f,  // 1s between I frames
        profileLevelColorBuilder: VideoProfileLevelColor.Builder.() -> Unit = {},
        customize: MediaFormatCustomHandler = {}
    ) : this(
        mimeType,
        startBitrate,
        resolution,
        fps,
        gopDurationInS,
        VideoProfileLevelColor.Builder(mimeType).apply {
            this.profileLevelColorBuilder()
        }.build(),
        customize
    )

    /**
     * Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to default level.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val level: Int = profileLevelColor.level

    /**
     * The dynamic range profile.
     * It is deduced from the [profile].
     * **See Also:** [DynamicRangeProfiles](https://developer.android.com/reference/android/hardware/camera2/params/DynamicRangeProfiles)
     */
    val dynamicRangeProfile = profileLevelColor.dynamicRangeProfile

    /**
     * Whether the configuration is HDR or not.
     *
     * @return true if the configuration is HDR
     */
    val isHdr = profileLevelColor.isHdr

    init {
        require(mimeType.isVideo) { "MimeType must be video" }
        require(startBitrate > 0) { "Bitrate must be > 0" }
        require(resolution.width > 0 && resolution.height > 0) { "Resolution width and height must be > 0" }
        require(fps > 0) { "FPS must be > 0" }
        require(gopDurationInS >= 0f) { "GOP duration must be >= 0" }
        if (isHdr) {
            require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "HDR encoding is only supported on Android 13+"
            }
            require(profileLevelColor.colorStandard == MediaFormat.COLOR_STANDARD_BT2020) {
                "Color standard must be COLOR_STANDARD_BT2020 for HDR profile"
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val colorStandard = profileLevelColor.colorStandard
                require(
                    colorStandard == 0 ||  // 0 is unspecified
                            colorStandard == MediaFormat.COLOR_STANDARD_BT709 ||
                            colorStandard == MediaFormat.COLOR_STANDARD_BT601_PAL ||
                            colorStandard == MediaFormat.COLOR_STANDARD_BT601_NTSC
                ) {
                    "Color standard must be COLOR_STANDARD_BT709, COLOR_STANDARD_BT601_PAL or COLOR_STANDARD_BT601_NTSC for SDR profile"
                }
            }
        }
    }

    /**
     * Get the media format from the video configuration
     *
     * @param requestFallback whether to request a fallback format if the preferred one is not supported
     * @return the corresponding video media format
     */
    override fun getFormat(requestFallback: Boolean): MediaFormat {
        val format = MediaFormat.createVideoFormat(
            mimeType, resolution.width, resolution.height
        )

        // Extended video format
        format.setInteger(MediaFormat.KEY_BIT_RATE, startBitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, gopDurationInS)
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, gopDurationInS.roundToInt())
        }

        if (!requestFallback) {
            format.setInteger(MediaFormat.KEY_PROFILE, profile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                format.setInteger(MediaFormat.KEY_LEVEL, level)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                format.setInteger(
                    MediaFormat.KEY_COLOR_STANDARD, profileLevelColor.colorStandard
                )
                format.setInteger(
                    MediaFormat.KEY_COLOR_RANGE,
                    profileLevelColor.dynamicRangeProfile.colorRange
                )
                format.setInteger(
                    MediaFormat.KEY_COLOR_TRANSFER, dynamicRangeProfile.transferFunction
                )
                if (isHdr) {
                    format.setFeatureEnabled(
                        MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing, true
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            format.setInteger(KEY_PRIORITY, 0) // Realtime hint
        }

        format.customize(requestFallback)

        return format
    }

    /**
     * Copies video configuration with new values
     */
    fun copy(
        mimeType: String = this.mimeType,
        startBitrate: Int = this.startBitrate,
        resolution: Size = this.resolution,
        fps: Int = this.fps,
        gopDurationInS: Float = this.gopDurationInS,
        profileLevelColor: VideoProfileLevelColor = this.profileLevelColor,
        customize: MediaFormatCustomHandler = this.customize
    ) = VideoCodecConfig(
        mimeType,
        startBitrate,
        resolution,
        fps,
        gopDurationInS,
        profileLevelColor,
        customize
    )

    override fun toString() =
        "VideoConfig(mimeType='$mimeType', startBitrate=$startBitrate, resolution=$resolution, fps=$fps, profileLevelColor=$profileLevelColor, gopDurationInS=$gopDurationInS)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoCodecConfig) return false

        if (!super.equals(other)) return false
        if (resolution != other.resolution) return false
        if (fps != other.fps) return false
        if (level != other.level) return false
        if (gopDurationInS != other.gopDurationInS) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mimeType.hashCode()
        result = 31 * result + startBitrate
        result = 31 * result + resolution.hashCode()
        result = 31 * result + fps
        result = 31 * result + profile
        result = 31 * result + level
        result = 31 * result + gopDurationInS.hashCode()
        return result
    }

    companion object {
        internal val DEFAULT_RESOLUTION = Size(1280, 720)
        internal const val DEFAULT_FPS = 30

        /**
         * Return the best bitrate according to resolution
         *
         * @param resolution the resolution
         * @return the corresponding bitrate
         */
        fun getBestBitrate(resolution: Size): Int {
            val numOfPixels = resolution.width * resolution.height
            return when {
                numOfPixels <= 320 * 240 -> 800_000
                numOfPixels <= 640 * 480 -> 1_000_000
                numOfPixels <= 1280 * 720 -> 2_000_000
                numOfPixels <= 1920 * 1080 -> 3_500_000
                else -> 4_000_000
            }
        }

        // Higher priority first
        private val h263ProfilePriority by lazy {
            listOf(
                H263ProfileBaseline
            )
        }

        private val avcProfilePriority by lazy {
            listOf(
                AVCProfileHigh,
                AVCProfileMain,
                AVCProfileExtended,
                AVCProfileBaseline,
                AVCProfileConstrainedHigh,
                AVCProfileConstrainedBaseline
            )
        }

        private val hevcProfilePriority by lazy {
            listOf(
                HEVCProfileMain
            )
        }

        private val vp8ProfilePriority by lazy {
            listOf(
                VP8ProfileMain
            )
        }

        private val vp9ProfilePriority by lazy {
            listOf(
                VP9Profile0
            )
        }

        private val av1ProfilePriority by lazy {
            listOf(
                AV1ProfileMain8
            )
        }

        private val apvProfilePriority by lazy {
            listOf(
                APVProfile422_10
            )
        }

        /**
         * Return the higher profile with the higher level
         */
        fun getBestProfile(mimeType: String): Int {
            val profilePriority = when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_H263 -> h263ProfilePriority
                MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfilePriority
                MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfilePriority
                MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9ProfilePriority
                MediaFormat.MIMETYPE_VIDEO_VP8 -> vp8ProfilePriority
                MediaFormat.MIMETYPE_VIDEO_AV1 -> av1ProfilePriority
                MediaFormat.MIMETYPE_VIDEO_APV -> apvProfilePriority
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
}

/**
 * Rotates video configuration to [rotation] from device natural orientation.
 */
fun VideoCodecConfig.rotateFromNaturalOrientation(context: Context, @RotationValue rotation: Int) =
    rotateDegreesFromNaturalOrientation(context, rotation.rotationToDegrees)

/**
 * Rotates video configuration to [rotationDegrees] from device natural orientation.
 */
fun VideoCodecConfig.rotateDegreesFromNaturalOrientation(
    context: Context, @IntRange(from = 0, to = 359) rotationDegrees: Int
): VideoCodecConfig {
    val newResolution = resolution.rotateFromNaturalOrientation(context, rotationDegrees)
    return if (resolution != newResolution) {
        copy(resolution = newResolution)
    } else {
        this
    }
}

/**
 * Video profile level with color information.
 */
sealed class VideoProfileLevelColor(
    /**
     * Video encoder profile. Encoders may not support requested profile. In this case, StreamPack fallbacks to default profile.
     * If not set, profile is always a 8 bit profile. StreamPack try to apply the highest profile available.
     * If the decoder does not support the profile, you should explicitly set the profile to a lower
     * value such as [AVCProfileBaseline] for AVC, [HEVCProfileMain] for HEVC, [VP9Profile0] for VP9.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val profile: Int,
    /**
     * Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to default level.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val level: Int,

    /**
     * The color standard.
     * Default is [MediaFormat.COLOR_STANDARD_BT709].
     * **See Also:** [MediaFormat COLOR_STANDARD_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val colorStandard: Int,
    /**
     * The dynamic range profile.
     * It is deduced from the [profile].
     * **See Also:** [DynamicRangeProfiles](https://developer.android.com/reference/android/hardware/camera2/params/DynamicRangeProfiles)
     */
    val dynamicRangeProfile: DynamicRangeProfile
) {
    /**
     * Whether the configuration is HDR or not.
     *
     * @return true if the configuration is HDR
     */
    val isHdr by lazy { dynamicRangeProfile != DynamicRangeProfile.sdr }

    companion object {
        internal class Sdr(
            profile: Int,
            level: Int,
            @SdrColorStandardValue colorStandard: Int
        ) : VideoProfileLevelColor(
            profile,
            level,
            colorStandard,
            DynamicRangeProfile.sdr
        ) {
            override fun toString(): String {
                return "VideoProfileLevelColor.Sdr(profile=$profile, level=$level, colorStandard=$colorStandard)"
            }
        }

        internal class Hdr(
            profile: Int,
            level: Int,
            dynamicRangeProfile: DynamicRangeProfile
        ) :
            VideoProfileLevelColor(
                profile,
                level,
                MediaFormat.COLOR_STANDARD_BT2020,
                dynamicRangeProfile
            ) {
            override fun toString(): String {
                return "VideoProfileLevelColor.Hdr(profile=$profile, level=$level, dynamicRangeProfile=$dynamicRangeProfile)"
            }
        }
    }

    /**
     * Builder for [VideoProfileLevelColor].
     *
     * @param mimeType Video encoder mime type.
     */
    class Builder internal constructor(private val mimeType: String) {
        /**
         * The encoder profile.
         */
        var profile: Int? = null

        /**
         * The encoder level.
         */
        var level: Int? = null

        /**
         * The color standard.
         * Only applicable for SDR profile.
         * Default is [MediaFormat.COLOR_STANDARD_BT709].
         * **See Also:** [MediaFormat COLOR_STANDARD_*](https://developer.android.com/reference/android/media/MediaFormat)
         */
        @SdrColorStandardValue
        var sdrColorStandard: Int = 0 // 0 is unspecified

        fun build(): VideoProfileLevelColor {
            val profile = profile ?: getBestProfile(mimeType)
            val level = level ?: getBestLevel(mimeType, profile)
            val dynamicRangeProfile = try {
                DynamicRangeProfile.fromProfile(mimeType, profile)
            } catch (_: Throwable) {
                // Fallback to SDR if no dynamic range profile found
                DynamicRangeProfile.sdr
            }
            return if (dynamicRangeProfile == DynamicRangeProfile.sdr) {
                Sdr(profile, level, sdrColorStandard)
            } else {
                require(sdrColorStandard == 0) {
                    "Color standard can only be set for SDR profile"
                }
                Hdr(profile, level, dynamicRangeProfile)
            }
        }
    }
}