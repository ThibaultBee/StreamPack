package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

import android.media.MediaCodecInfo

enum class Profile(val value: Int) {
    PROFILE_0(0),
    PROFILE_1(1),
    PROFILE_2(2),
    PROFILE_3(3);

    companion object {
        fun fromValue(value: Int) = entries.first { it.value == value }

        fun fromMediaFormat(mediaCodecProfile: Int) = when (mediaCodecProfile) {
            MediaCodecInfo.CodecProfileLevel.VP9Profile0 -> PROFILE_0
            MediaCodecInfo.CodecProfileLevel.VP9Profile1 -> PROFILE_1
            MediaCodecInfo.CodecProfileLevel.VP9Profile2 -> PROFILE_2
            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR -> PROFILE_2
            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus -> PROFILE_2
            MediaCodecInfo.CodecProfileLevel.VP9Profile3 -> PROFILE_3
            MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR -> PROFILE_3
            MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus -> PROFILE_3
            else -> throw IllegalArgumentException("Unknown profile: $mediaCodecProfile")
        }
    }
}