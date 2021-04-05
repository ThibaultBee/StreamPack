package com.github.thibaultbee.streampack.data

import android.media.MediaCodecInfo.CodecProfileLevel
import android.util.Size
import com.github.thibaultbee.streampack.utils.isVideo

data class VideoConfig(
    val mimeType: String,
    val startBitrate: Int,
    val resolution: Size,
    val fps: Int,
    val profile: Int = CodecProfileLevel.AVCProfileHigh,
    val level: Int = CodecProfileLevel.AVCLevel52,
) {
    init {
        require(mimeType.isVideo())
    }
}

