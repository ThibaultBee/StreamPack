package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.muxers.IMuxerListener

open class TSOutputCallback(private val muxerListener: IMuxerListener) {
    protected fun writePacket(packet: Packet) {
        packet.buffer.rewind()
        return muxerListener.onOutputFrame(packet)
    }
}