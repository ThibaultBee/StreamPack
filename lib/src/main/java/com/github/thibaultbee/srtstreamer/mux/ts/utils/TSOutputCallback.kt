package com.github.thibaultbee.srtstreamer.mux.ts.utils

import com.github.thibaultbee.srtstreamer.interfaces.MuxListener
import java.nio.ByteBuffer

open class TSOutputCallback(var muxListener: MuxListener) {
    protected fun writePacket(buffer: ByteBuffer) {
        buffer.rewind()
        return muxListener.onOutputFrame(buffer)
    }
}