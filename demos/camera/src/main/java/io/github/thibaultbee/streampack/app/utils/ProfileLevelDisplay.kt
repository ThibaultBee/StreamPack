package io.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.media.MediaCodecInfo.CodecProfileLevel.*
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.app.R

class ProfileLevelDisplay(context: Context) {
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
            HEVCHighTierLevel41 to context.getString(R.string.video_level_main_level41),
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
            HEVCHighTierLevel62 to context.getString(R.string.video_level_main_level62),
        )

    fun getProfileName(mimeType: String, profile: Int): String {
        val nameMap = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfileNameMap
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfileNameMap
            else -> emptyMap()
        }
        return try {
            nameMap[profile]!!
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun getProfile(mimeType: String, name: String): Int {
        val nameMap = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfileNameMap
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfileNameMap
            else -> emptyMap()
        }
        return nameMap.entries.find { it.value == name }!!.key
    }

    fun getLevelName(mimeType: String, level: Int): String {
        val nameMap = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcLevelNameMap
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcLevelNameMap
            else -> emptyMap()
        }
        return try {
            nameMap[level]!!
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun getLevel(mimeType: String, name: String): Int {
        val nameMap = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> avcLevelNameMap
            MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcLevelNameMap
            else -> emptyMap()
        }
        return nameMap.entries.find { it.value == name }!!.key
    }
}