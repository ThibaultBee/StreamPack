package com.github.thibaultbee.streampack.muxers.ts.data

data class Stream(
    val mimeType: String,
    val pid: Short,
    val discontinuity: Boolean = false
)
