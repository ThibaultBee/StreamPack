package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.muxers.IMuxerListener
import java.nio.ByteBuffer

open class TSOutputCallback(var muxerListener: IMuxerListener) {
    protected fun writePacket(buffer: ByteBuffer) {
        buffer.rewind()
        return muxerListener.onOutputFrame(buffer)
    }
}