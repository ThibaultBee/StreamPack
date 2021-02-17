package com.github.thibaultbee.srtstreamer.muxers.ts.tables

import com.github.thibaultbee.srtstreamer.muxers.IMuxerListener
import com.github.thibaultbee.srtstreamer.muxers.ts.packets.TS
import com.github.thibaultbee.srtstreamer.muxers.ts.utils.CRC32
import java.nio.ByteBuffer

open class Psi(
    muxerListener: IMuxerListener,
    pid: Short,
    private val tableId: Byte,
    private val sectionSyntaxIndicator: Boolean = false,
    private val reservedFutureUse: Boolean = false,
    private val tableIdExtension: Short = 0,
    var versionNumber: Byte = 0,
    private val sectionNumber: Byte = 0,
    private val lastSectionNumber: Byte = 0,
) : TS(muxerListener, pid) {
    companion object {
        const val CRC_SIZE = 4
        const val PSI_HEADER_SIZE = 9 // contains pointer_field
    }

    protected fun write(buffer: ByteBuffer) {
        write(payload = asByteBuffer(buffer))
    }

    fun asByteBuffer(payload: ByteBuffer): ByteBuffer {
        val table =
            ByteBuffer.allocate(payload.limit() + PSI_HEADER_SIZE + CRC_SIZE) // + Header & CRC

        table.put(0) // pointer_field

        val tableHeader = TableHeader(
            tableId = tableId,
            payloadLength = payload.remaining().toShort(),
            tableIdExtension = tableIdExtension,
            sectionSyntaxIndicator = sectionSyntaxIndicator,
            reservedFutureUse = reservedFutureUse,
            versionNumber = versionNumber,
            sectionNumber = sectionNumber,
            lastSectionNumber = lastSectionNumber
        )

        table.put(tableHeader.asByteBuffer())

        table.put(payload)

        val crc32 = CRC32.get(
            table,
            1,
            table.position()
        ) // offset = 1 -> pointer_field is not in CRC32 computation
        table.put((crc32 shr 24 and 0xFF).toByte())
        table.put((crc32 shr 16 and 0xFF).toByte())
        table.put((crc32 shr 8 and 0xFF).toByte())
        table.put((crc32 and 0xFF).toByte())

        table.rewind()

        return table
    }
}