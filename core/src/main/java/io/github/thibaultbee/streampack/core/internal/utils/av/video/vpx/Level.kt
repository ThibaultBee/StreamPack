package io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx

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

enum class Level(val value: Int) {
    UNDEFINED(0),
    LEVEL_1(10),
    LEVEL_11(11),
    LEVEL_2(20),
    LEVEL_21(21),
    LEVEL_3(30),
    LEVEL_31(31),
    LEVEL_4(40),
    LEVEL_41(41),
    LEVEL_5(50),
    LEVEL_51(51),
    LEVEL_52(52),
    LEVEL_6(60),
    LEVEL_61(61),
    LEVEL_62(62);

    companion object {
        fun fromValue(value: Int) = entries.first { it.value == value }

        fun fromMediaFormat(format: Int): Level {
            return when (format) {
                VP9Level1 -> LEVEL_1
                VP9Level11 -> LEVEL_11
                VP9Level2 -> LEVEL_2
                VP9Level21 -> LEVEL_21
                VP9Level3 -> LEVEL_3
                VP9Level31 -> LEVEL_31
                VP9Level4 -> LEVEL_4
                VP9Level41 -> LEVEL_41
                VP9Level5 -> LEVEL_5
                VP9Level51 -> LEVEL_51
                VP9Level52 -> LEVEL_52
                VP9Level6 -> LEVEL_6
                VP9Level61 -> LEVEL_61
                VP9Level62 -> LEVEL_62
                else -> UNDEFINED
            }
        }
    }
}