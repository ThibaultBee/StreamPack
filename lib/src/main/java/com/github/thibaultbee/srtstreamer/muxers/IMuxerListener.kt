package com.github.thibaultbee.srtstreamer.muxers

import java.nio.ByteBuffer

interface IMuxerListener {
    fun onOutputFrame(buffer: ByteBuffer)
}