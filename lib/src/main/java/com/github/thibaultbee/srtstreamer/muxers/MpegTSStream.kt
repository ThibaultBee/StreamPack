package com.github.thibaultbee.srtstreamer.muxers

data class MpegTSStream(
    val mimeType: String,
    val pid: Int,
    var cc: Int = 15,
    var discontinuity: Int = 0
)
