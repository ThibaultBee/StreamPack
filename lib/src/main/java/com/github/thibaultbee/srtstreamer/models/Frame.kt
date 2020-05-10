package com.github.thibaultbee.srtstreamer.models

import java.nio.ByteBuffer

data class Frame (
    var buffer: ByteBuffer,
    val mimeType: String,
    var pts: Long,
    var dts: Long = -1,
    val isKeyFrame: Boolean = false,
    val isCodecData: Boolean = false,
    val extra: ByteBuffer? = null,
    var pid: Int = -1)