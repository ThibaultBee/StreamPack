package com.github.thibaultbee.streampack.muxers

import java.nio.ByteBuffer

interface IMuxerListener {
    fun onOutputFrame(buffer: ByteBuffer)
}