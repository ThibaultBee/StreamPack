package com.github.thibaultbee.srtstreamer.data

import android.util.Size

data class VideoConfig(
    var mimeType: String,
    var startBitrate: Int,
    var resolution: Size,
    var fps: Int
)

