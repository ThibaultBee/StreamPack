package io.github.thibaultbee.streampack.core.elements.utils.av.video

import android.hardware.camera2.params.DynamicRangeProfiles
import android.media.MediaCodecInfo.CodecProfileLevel.APVProfile422_10
import android.media.MediaCodecInfo.CodecProfileLevel.APVProfile422_10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.APVProfile422_10HDR10Plus
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
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.VP8ProfileMain
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

data class DynamicRangeProfile(
    val dynamicRange: Long,
    val transferFunction: Int,
    val colorRange: Int
) {
    val isHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        dynamicRange != DynamicRangeProfiles.STANDARD
    } else {
        false
    }

    companion object {
        val sdr =
            DynamicRangeProfile(
                DynamicRangeProfiles.STANDARD,
                MediaFormat.COLOR_TRANSFER_SDR_VIDEO,
                MediaFormat.COLOR_RANGE_LIMITED
            )
        val hdr = DynamicRangeProfile(
            DynamicRangeProfiles.HLG10,
            MediaFormat.COLOR_TRANSFER_HLG,
            MediaFormat.COLOR_RANGE_FULL
        )
        val hdr10 =
            DynamicRangeProfile(
                DynamicRangeProfiles.HDR10,
                MediaFormat.COLOR_TRANSFER_ST2084,
                MediaFormat.COLOR_RANGE_FULL
            )
        val hdr10Plus =
            DynamicRangeProfile(
                DynamicRangeProfiles.HDR10_PLUS,
                MediaFormat.COLOR_TRANSFER_ST2084,
                MediaFormat.COLOR_RANGE_FULL
            )

        private val avcProfilesMap = mapOf(
            AVCProfileBaseline to sdr,
            AVCProfileConstrainedBaseline to sdr,
            AVCProfileConstrainedHigh to sdr,
            AVCProfileExtended to sdr,
            AVCProfileHigh to sdr,
            AVCProfileHigh10 to hdr,
            AVCProfileHigh422 to sdr,
            AVCProfileHigh444 to sdr,
            AVCProfileMain to sdr,
        )

        private val hevcProfilesMap = mapOf(
            HEVCProfileMain to sdr,
            HEVCProfileMain10 to hdr,
            HEVCProfileMain10HDR10 to hdr10,
            HEVCProfileMain10HDR10Plus to hdr10Plus,
        )

        private val vp8ProfilesMap = mapOf(VP8ProfileMain to sdr)

        private val vp9ProfilesMap = mapOf(
            VP9Profile0 to sdr,
            VP9Profile1 to hdr,
            VP9Profile2 to hdr,
            VP9Profile2HDR to hdr,
            VP9Profile2HDR10Plus to hdr10Plus,
            VP9Profile3 to hdr,
            VP9Profile3HDR to hdr10,
            VP9Profile3HDR10Plus to hdr10Plus,
        )

        private val av1ProfilesMap = mapOf(
            AV1ProfileMain8 to sdr,
            AV1ProfileMain10 to hdr,
            AV1ProfileMain10HDR10 to hdr10,
            AV1ProfileMain10HDR10Plus to hdr10Plus,
        )

        private val apvProfilesMap = mapOf(
            APVProfile422_10 to hdr,
            APVProfile422_10HDR10 to hdr10,
            APVProfile422_10HDR10Plus to hdr10Plus,
        )

        fun fromProfile(mimetype: String, profile: Int): DynamicRangeProfile {
            return when (mimetype) {
                MediaFormat.MIMETYPE_VIDEO_H263 -> sdr
                MediaFormat.MIMETYPE_VIDEO_AVC -> avcProfilesMap[profile]
                MediaFormat.MIMETYPE_VIDEO_HEVC -> hevcProfilesMap[profile]
                MediaFormat.MIMETYPE_VIDEO_VP8 -> vp8ProfilesMap[profile]
                MediaFormat.MIMETYPE_VIDEO_VP9 -> vp9ProfilesMap[profile]
                MediaFormat.MIMETYPE_VIDEO_AV1 -> av1ProfilesMap[profile]
                MediaFormat.MIMETYPE_VIDEO_APV -> apvProfilesMap[profile]
                else -> throw IllegalArgumentException("Unknown mimetype $mimetype")
            } ?: throw IllegalArgumentException("Profile $profile is not supported for $mimetype")
        }
    }
}