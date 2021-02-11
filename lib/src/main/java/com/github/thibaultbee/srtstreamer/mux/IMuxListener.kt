package com.github.thibaultbee.srtstreamer.mux

import java.nio.ByteBuffer

interface IMuxListener {
    fun onOutputFrame(buffer: ByteBuffer)
}