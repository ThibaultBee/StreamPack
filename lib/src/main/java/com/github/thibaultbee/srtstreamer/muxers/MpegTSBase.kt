package com.github.thibaultbee.srtstreamer.muxers

import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

open class MpegTSBase(val logger: Logger, var muxerListener: MuxerListener?) {
    companion object {
        const val TS_PACKET_SIZE = 188
    }

    protected fun writePacket(buffer: ByteBuffer): Error {
        buffer.rewind()
        return muxerListener?.onOutputFrame(buffer) ?: Error.INVALID_OPERATION
    }
}