package com.github.thibaultbee.srtstreamer.muxers.ts.data

data class Stream(
    val mimeType: String,
    val pid: Short,
    val discontinuity: Boolean = false
)
