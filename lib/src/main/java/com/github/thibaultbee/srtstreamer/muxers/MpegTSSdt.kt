package com.github.thibaultbee.srtstreamer.muxers

import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer


class MpegTSSdt(
    logger: Logger,
    muxerListener: MuxerListener,
    val tablesVersion: Int = 0,
    val tsId: Int = 1,
    val originalNetworkId: Int = 0xff01
) : MpegTSSection(logger, muxerListener, SDT_PID) {
    fun write(services: List<MpegTSService>): Error {
        val buffer = ByteBuffer.allocate(SECTION_LENGTH)

        buffer.put(originalNetworkId.toByte())
        buffer.put(0xFF.toByte())

        services.forEach { service ->
            buffer.putShort(service.sid.toShort())
            buffer.put(0xfc.toByte())

            val descListLenPosition = buffer.position()
            buffer.position(buffer.position() + 2)

            // write only one descriptor for the service name and provider
            buffer.put(0x48.toByte())

            val descLenPosition = buffer.position()
            buffer.position(buffer.position() + 1)

            buffer.put(service.type.value.toByte())

            buffer.put(service.providerName.length.toByte())
            buffer.put(service.providerName.toByteArray())

            buffer.put(service.name.length.toByte())
            buffer.put(service.name.toByteArray())

            buffer.put(descLenPosition, (buffer.position() - descLenPosition - 1).toByte())

            /* fill descriptor length */
            val runningStatus = 4 /* running */
            val freeCaMode = 0
            val short = (runningStatus shl 13) or (freeCaMode shl 12) or
                    (buffer.position() - descListLenPosition - 2)
            buffer.put(descListLenPosition, (short shr 8).toByte())
            buffer.put(descListLenPosition + 1, (short).toByte())
        }

        buffer.limit(buffer.position())
        buffer.rewind()
        writeSection1(buffer, SDT_TID, tsId, tablesVersion, 0, 0)

        return Error.SUCCESS
    }
}