package com.github.thibaultbee.srtstreamer.mux.ts.utils

import com.github.thibaultbee.srtstreamer.mux.IMuxListener
import java.nio.ByteBuffer

open class TSOutputCallback(var muxListener: IMuxListener) {
    protected fun writePacket(buffer: ByteBuffer) {
        buffer.rewind()
        return muxListener.onOutputFrame(buffer)
    }
}