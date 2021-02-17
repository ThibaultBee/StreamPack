package com.github.thibaultbee.streampack.data

import android.util.Size

data class VideoConfig(
    var mimeType: String,
    var startBitrate: Int,
    var resolution: Size,
    var fps: Int
)

