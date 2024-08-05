package io.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectELD
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectERLC
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectERScalable
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectHE
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLD
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLTP
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectMain
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectSSR
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectScalable
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectXHE
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level2
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level21
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level22
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level23
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level3
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level31
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level32
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level33
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level4
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level41
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level42
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level43
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level5
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level51
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level52
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level53
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level6
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level61
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level62
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level63
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level7
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level71
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level72
import android.media.MediaCodecInfo.CodecProfileLevel.AV1Level73
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel1
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel11
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel12
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel13
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel1b
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel2
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel21
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel22
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel3
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel32
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel4
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel41
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel42
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel5
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel51
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel52
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel6
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel61
import android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel62
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileExtended
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level10
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level20
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level30
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level40
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level45
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level50
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level60
import android.media.MediaCodecInfo.CodecProfileLevel.H263Level70
import android.media.MediaCodecInfo.CodecProfileLevel.H263ProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel1
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel2
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel21
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel3
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel4
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel41
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel5
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel51
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel6
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel61
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel2
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel21
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel3
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel31
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel4
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel5
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel52
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel6
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel61
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill
import android.media.MediaCodecInfo.CodecProfileLevel.VP8Level_Version0
import android.media.MediaCodecInfo.CodecProfileLevel.VP8Level_Version1
import android.media.MediaCodecInfo.CodecProfileLevel.VP8Level_Version2
import android.media.MediaCodecInfo.CodecProfileLevel.VP8Level_Version3
import android.media.MediaCodecInfo.CodecProfileLevel.VP8ProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level1
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level11
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level2
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level21
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level3
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level31
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level4
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level41
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level5
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level51
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level52
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level6
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level61
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Level62
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile1
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.app.R

/**
 * Get a string representation of a profile level.
 *
 * @param context The application context.
 */
class ProfileLevelDisplay(private val context: Context) {
    private val aacProfileNameMap =
        mutableMapOf(
            AACObjectMain to context.getString(R.string.audio_profile_main),
            AACObjectLC to context.getString(R.string.audio_profile_lc),
            AACObjectSSR to context.getString(R.string.audio_profile_ssr),
            AACObjectLTP to context.getString(R.string.audio_profile_ltp),
            AACObjectHE to context.getString(R.string.audio_profile_he),
            AACObjectScalable to context.getString(R.string.audio_profile_scalable),
            AACObjectERLC to context.getString(R.string.audio_profile_er_lc),
            AACObjectLD to context.getString(R.string.audio_profile_ld),
            AACObjectHE_PS to context.getString(R.string.audio_profile_he_ps),
            AACObjectELD to context.getString(R.string.audio_profile_eld)
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putAll(
                    mapOf(
                        AACObjectERScalable to context.getString(R.string.audio_profile_er_scalable)
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                putAll(
                    mapOf(
                        AACObjectXHE to context.getString(R.string.audio_profile_xhe)
                    )
                )
            }
        }

    private val h263ProfileNameMap = mapOf(
        H263ProfileBaseline to context.getString(R.string.video_profile_baseline)
    )

    private val avcProfileNameMap =
        mutableMapOf(
            AVCProfileBaseline to context.getString(R.string.video_profile_baseline),
            AVCProfileExtended to context.getString(R.string.video_profile_extended),
            AVCProfileMain to context.getString(R.string.video_profile_main),
            AVCProfileHigh to context.getString(R.string.video_profile_high),
            AVCProfileHigh10 to context.getString(R.string.video_profile_high10),
            AVCProfileHigh422 to context.getString(R.string.video_profile_high422)
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                putAll(
                    mapOf(
                        AVCProfileConstrainedBaseline to context.getString(R.string.video_profile_constrained_baseline),
                        AVCProfileConstrainedHigh to context.getString(R.string.video_profile_constrained_high)
                    )
                )
            }
        }

    private val hevcProfileNameMap =
        mutableMapOf(
            HEVCProfileMain to context.getString(R.string.video_profile_main),
            HEVCProfileMain10 to context.getString(R.string.video_profile_main10)
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                put(HEVCProfileMain10HDR10, context.getString(R.string.video_profile_main10_hdr10))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                put(HEVCProfileMainStill, context.getString(R.string.video_profile_main_still))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    HEVCProfileMain10HDR10Plus,
                    context.getString(R.string.video_profile_main10_hdr10_plus)
                )
            }
        }

    private val vp8ProfileNameMap =
        mapOf(VP8ProfileMain to context.getString(R.string.video_profile_main))

    private val vp9ProfileNameMap =
        mutableMapOf<Int, String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                put(VP9Profile0, context.getString(R.string.video_profile_vp9_profile0))
                put(VP9Profile1, context.getString(R.string.video_profile_vp9_profile1))
                put(VP9Profile2, context.getString(R.string.video_profile_vp9_profile2))
                put(VP9Profile2HDR, context.getString(R.string.video_profile_vp9_profile2_hdr10))
                put(
                    VP9Profile2HDR10Plus,
                    context.getString(R.string.video_profile_vp9_profile2_hrd10plus)
                )
                put(VP9Profile3, context.getString(R.string.video_profile_vp9_profile3))
                put(VP9Profile3HDR, context.getString(R.string.video_profile_vp9_profile3_hdr10))
                put(
                    VP9Profile3HDR10Plus,
                    context.getString(R.string.video_profile_vp9_profile3_hdr10plus)
                )
            }
        }

    private val av1ProfileNameMap =
        mutableMapOf<Int, String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(AV1ProfileMain8, context.getString(R.string.video_profile_main))
                put(AV1ProfileMain10, context.getString(R.string.video_profile_main10))
                put(AV1ProfileMain10HDR10, context.getString(R.string.video_profile_main10_hdr10))
                put(
                    AV1ProfileMain10HDR10Plus,
                    context.getString(R.string.video_profile_main10_hdr10_plus)
                )
            }
        }

    private val h263LevelNameMap =
        mutableMapOf(
            H263Level10 to context.getString(R.string.video_level_10),
            H263Level20 to context.getString(R.string.video_level_20),
            H263Level30 to context.getString(R.string.video_level_30),
            H263Level40 to context.getString(R.string.video_level_40),
            H263Level45 to context.getString(R.string.video_level_45),
            H263Level50 to context.getString(R.string.video_level_50),
            H263Level60 to context.getString(R.string.video_level_60),
            H263Level70 to context.getString(R.string.video_level_70)
        )

    private val avcLevelNameMap =
        mutableMapOf(
            AVCLevel1 to context.getString(R.string.video_level_1),
            AVCLevel1b to context.getString(R.string.video_level_1b),
            AVCLevel11 to context.getString(R.string.video_level_11),
            AVCLevel12 to context.getString(R.string.video_level_12),
            AVCLevel13 to context.getString(R.string.video_level_13),
            AVCLevel2 to context.getString(R.string.video_level_2),
            AVCLevel21 to context.getString(R.string.video_level_21),
            AVCLevel22 to context.getString(R.string.video_level_22),
            AVCLevel3 to context.getString(R.string.video_level_3),
            AVCLevel31 to context.getString(R.string.video_level_31),
            AVCLevel32 to context.getString(R.string.video_level_32),
            AVCLevel4 to context.getString(R.string.video_level_4),
            AVCLevel41 to context.getString(R.string.video_level_41),
            AVCLevel42 to context.getString(R.string.video_level_42),
            AVCLevel5 to context.getString(R.string.video_level_5),
            AVCLevel51 to context.getString(R.string.video_level_51),
            AVCLevel52 to context.getString(R.string.video_level_52),
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                putAll(
                    mapOf(
                        AVCLevel6 to context.getString(R.string.video_level_6),
                        AVCLevel61 to context.getString(R.string.video_level_61),
                        AVCLevel62 to context.getString(R.string.video_level_62)
                    )
                )
            }
        }

    private val hevcLevelNameMap =
        mutableMapOf(
            HEVCMainTierLevel1 to context.getString(R.string.video_level_main_level1),
            HEVCHighTierLevel1 to context.getString(R.string.video_level_high_level1),
            HEVCMainTierLevel2 to context.getString(R.string.video_level_main_level2),
            HEVCHighTierLevel2 to context.getString(R.string.video_level_high_level2),
            HEVCMainTierLevel21 to context.getString(R.string.video_level_main_level21),
            HEVCHighTierLevel21 to context.getString(R.string.video_level_high_level21),
            HEVCMainTierLevel3 to context.getString(R.string.video_level_main_level3),
            HEVCHighTierLevel3 to context.getString(R.string.video_level_high_level3),
            HEVCMainTierLevel31 to context.getString(R.string.video_level_main_level31),
            HEVCHighTierLevel31 to context.getString(R.string.video_level_high_level31),
            HEVCMainTierLevel4 to context.getString(R.string.video_level_main_level4),
            HEVCHighTierLevel4 to context.getString(R.string.video_level_high_level4),
            HEVCMainTierLevel41 to context.getString(R.string.video_level_main_level41),
            HEVCHighTierLevel41 to context.getString(R.string.video_level_high_level41),
            HEVCMainTierLevel5 to context.getString(R.string.video_level_main_level5),
            HEVCHighTierLevel5 to context.getString(R.string.video_level_high_level5),
            HEVCMainTierLevel51 to context.getString(R.string.video_level_main_level51),
            HEVCHighTierLevel51 to context.getString(R.string.video_level_high_level51),
            HEVCMainTierLevel52 to context.getString(R.string.video_level_main_level52),
            HEVCHighTierLevel52 to context.getString(R.string.video_level_high_level52),
            HEVCMainTierLevel6 to context.getString(R.string.video_level_main_level6),
            HEVCHighTierLevel6 to context.getString(R.string.video_level_high_level6),
            HEVCMainTierLevel61 to context.getString(R.string.video_level_main_level61),
            HEVCHighTierLevel61 to context.getString(R.string.video_level_high_level61),
            HEVCMainTierLevel62 to context.getString(R.string.video_level_main_level62),
            HEVCHighTierLevel62 to context.getString(R.string.video_level_high_level62),
        )

    private val vp8LevelNameMap = mapOf(
        VP8Level_Version0 to context.getString(R.string.video_level_version0),
        VP8Level_Version1 to context.getString(R.string.video_level_version1),
        VP8Level_Version2 to context.getString(R.string.video_level_version2),
        VP8Level_Version3 to context.getString(R.string.video_level_version3)
    )

    private val vp9LevelNameMap =
        mutableMapOf<Int, String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putAll(
                    mapOf(
                        VP9Level1 to context.getString(R.string.video_level_1),
                        VP9Level11 to context.getString(R.string.video_level_11),
                        VP9Level2 to context.getString(R.string.video_level_2),
                        VP9Level21 to context.getString(R.string.video_level_21),
                        VP9Level3 to context.getString(R.string.video_level_3),
                        VP9Level31 to context.getString(R.string.video_level_31),
                        VP9Level4 to context.getString(R.string.video_level_4),
                        VP9Level41 to context.getString(R.string.video_level_41),
                        VP9Level5 to context.getString(R.string.video_level_5),
                        VP9Level51 to context.getString(R.string.video_level_51),
                        VP9Level52 to context.getString(R.string.video_level_52),
                        VP9Level6 to context.getString(R.string.video_level_6),
                        VP9Level61 to context.getString(R.string.video_level_61),
                        VP9Level62 to context.getString(R.string.video_level_62),
                    )
                )
            }
        }

    private val av1LevelNameMap =
        mutableMapOf<Int, String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                putAll(
                    mapOf(
                        AV1Level2 to context.getString(R.string.video_level_2),
                        AV1Level21 to context.getString(R.string.video_level_21),
                        AV1Level22 to context.getString(R.string.video_level_22),
                        AV1Level23 to context.getString(R.string.video_level_21),
                        AV1Level3 to context.getString(R.string.video_level_3),
                        AV1Level31 to context.getString(R.string.video_level_31),
                        AV1Level32 to context.getString(R.string.video_level_32),
                        AV1Level33 to context.getString(R.string.video_level_33),
                        AV1Level4 to context.getString(R.string.video_level_4),
                        AV1Level41 to context.getString(R.string.video_level_41),
                        AV1Level42 to context.getString(R.string.video_level_42),
                        AV1Level43 to context.getString(R.string.video_level_43),
                        AV1Level5 to context.getString(R.string.video_level_5),
                        AV1Level51 to context.getString(R.string.video_level_51),
                        AV1Level52 to context.getString(R.string.video_level_52),
                        AV1Level53 to context.getString(R.string.video_level_53),
                        AV1Level6 to context.getString(R.string.video_level_6),
                        AV1Level61 to context.getString(R.string.video_level_61),
                        AV1Level62 to context.getString(R.string.video_level_62),
                        AV1Level63 to context.getString(R.string.video_level_63),
                        AV1Level7 to context.getString(R.string.video_level_7),
                        AV1Level71 to context.getString(R.string.video_level_71),
                        AV1Level72 to context.getString(R.string.video_level_72),
                        AV1Level73 to context.getString(R.string.video_level_73),
                    )
                )
            }
        }

    private fun getProfileMap(mimeType: String) = when (mimeType) {
        MediaFormat.MIMETYPE_AUDIO_AAC -> aacProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_H263 -> h263ProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9ProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_VP8 -> vp8ProfileNameMap
        MediaFormat.MIMETYPE_VIDEO_AV1 -> av1ProfileNameMap
        else -> emptyMap()
    }

    fun getProfileName(mimeType: String, profile: Int): String {
        val nameMap = getProfileMap(mimeType)
        return try {
            nameMap[profile]!!
        } catch (_: Exception) {
            context.getString(R.string.av_profile_unknown)
        }
    }

    fun getProfile(mimeType: String, name: String): Int {
        val nameMap = getProfileMap(mimeType)
        return nameMap.entries.first { it.value == name }.key
    }

    private fun getLevelMap(mimeType: String) = when (mimeType) {
        MediaFormat.MIMETYPE_VIDEO_H263 -> h263LevelNameMap
        MediaFormat.MIMETYPE_VIDEO_AVC -> avcLevelNameMap
        MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcLevelNameMap
        MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9LevelNameMap
        MediaFormat.MIMETYPE_VIDEO_VP8 -> vp8LevelNameMap
        MediaFormat.MIMETYPE_VIDEO_AV1 -> av1LevelNameMap
        else -> emptyMap()
    }

    fun getLevelName(mimeType: String, level: Int): String {
        val nameMap = getLevelMap(mimeType)
        return try {
            nameMap[level]!!
        } catch (_: Exception) {
            context.getString(R.string.av_level_unknown)
        }
    }

    fun getLevel(mimeType: String, name: String): Int {
        val nameMap = getLevelMap(mimeType)
        return nameMap.entries.first { it.value == name }.key
    }

    fun getAllLevelSet(mimeType: String): Set<Int> {
        return getLevelMap(mimeType).keys
    }
}