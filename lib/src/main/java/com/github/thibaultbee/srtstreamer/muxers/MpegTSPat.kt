package com.github.thibaultbee.srtstreamer.muxers

import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

class MpegTSPat(
    logger: Logger,
    muxerListener: MuxerListener,
    val tablesVersion: Int = 0,
    val tsId: Int = 1
) : MpegTSSection(logger, muxerListener, PAT_PID) {
    fun write(services: List<MpegTSService>): Error {
        val buffer = ByteBuffer.allocate(SECTION_LENGTH)

        services.forEach {
            buffer.putShort(it.sid.toShort())
            buffer.putShort((0xe000 or it.pmt.pid).toShort())
        }

        buffer.limit(buffer.position())
        buffer.rewind()
        writeSection1(buffer, PAT_TID, tsId, tablesVersion, 0, 0)

        return Error.SUCCESS
    }
}