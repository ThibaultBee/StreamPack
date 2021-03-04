package com.github.thibaultbee.streampack.data

import android.util.Size
import com.github.thibaultbee.streampack.utils.isVideo

data class VideoConfig(
    var mimeType: String,
    var startBitrate: Int,
    var resolution: Size,
    var fps: Int
) {
    init {
        require(mimeType.isVideo())
    }
}

