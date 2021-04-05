package com.github.thibaultbee.streampack.data

import android.util.Size
import com.github.thibaultbee.streampack.utils.isVideo

data class VideoConfig(
    val mimeType: String,
    val startBitrate: Int,
    val resolution: Size,
    val fps: Int
) {
    init {
        require(mimeType.isVideo())
    }
}

