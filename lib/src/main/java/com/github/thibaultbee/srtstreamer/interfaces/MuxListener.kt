package com.github.thibaultbee.srtstreamer.interfaces

import java.nio.ByteBuffer

interface MuxListener {
    fun onOutputFrame(buffer: ByteBuffer)
}