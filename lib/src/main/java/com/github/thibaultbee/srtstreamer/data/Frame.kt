package com.github.thibaultbee.srtstreamer.data

import java.nio.ByteBuffer

data class Frame(
    var buffer: ByteBuffer,
    val mimeType: String,
    var pts: Long, // in µs
    var dts: Long? = null, // in µs
    val isKeyFrame: Boolean = false,
    val isCodecData: Boolean = false,
    val extra: ByteBuffer? = null
)