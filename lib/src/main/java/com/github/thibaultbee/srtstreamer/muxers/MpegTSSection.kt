package com.github.thibaultbee.srtstreamer.muxers

import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.experimental.or

open class MpegTSSection(
    logger: Logger,
    muxerListener: MuxerListener,
    val pid: Int,
    var cc: Int = 15,
    var discontinuity: Int = 0
) : MpegTSBase(logger, muxerListener) {

    companion object {
        const val SECTION_LENGTH = 1020

        // Table ids
        const val PAT_TID = 0x00
        const val PMT_TID = 0x02
        const val SDT_TID = 0x42

        // Pids
        const val PAT_PID = 0x0000
        const val SDT_PID = 0x0011
    }

    fun writeSection1(
        buffer: ByteBuffer,
        tid: Int,
        id: Int,
        version: Int,
        secNum: Int,
        lastSecNum: Int
    ): Error {
        val sectionBuffer = ByteBuffer.allocate(buffer.limit() + 12) // + Header & CRC

        val flags = if (tid == SDT_TID) {
            0xf000
        } else {
            0xb000
        }

        sectionBuffer.put(tid.toByte())
        sectionBuffer.putShort((flags or (buffer.limit() + 5 + 4)).toShort())
        sectionBuffer.putShort(id.toShort())
        sectionBuffer.put((0xc1 or (version shl 1)).toByte())
        sectionBuffer.put(secNum.toByte())
        sectionBuffer.put(lastSecNum.toByte())
        sectionBuffer.put(buffer)

        // CRC computation
        val crc32 = CRC32()
        crc32.update(sectionBuffer.array(), 0, sectionBuffer.position())
        val crc = ByteBuffer.allocate(4)
        crc.order(ByteOrder.LITTLE_ENDIAN)
        crc.putInt(crc32.value.toInt())
        crc.rewind()
        sectionBuffer.put(crc)

        sectionBuffer.rewind()

        return writeSection(sectionBuffer)
    }

    private fun writeSection(buffer: ByteBuffer): Error {
        val packet = ByteBuffer.allocateDirect(TS_PACKET_SIZE)
        val limit = buffer.limit()

        while (buffer.hasRemaining()) {
            packet.rewind()
            packet.put(0x47)
            var byte = pid shr 8
            if (buffer.position() == 0) { // first
                byte = byte or 0x40
            }
            packet.put(byte.toByte())
            packet.put(pid.toByte())

            cc = (cc + 1) and 0xF
            packet.put((0x10 or cc).toByte())

            if (discontinuity != 0) {
                packet.put(packet.position() - 1, packet.get(packet.position() - 1) or 0x20)
                packet.put(1.toByte())
                packet.put(0x80.toByte())
                discontinuity = 0
            }
            if (buffer.position() == 0) {
                packet.put(0)
            }

            buffer.limit(buffer.position() + Math.min(packet.remaining(), buffer.remaining()))
            packet.put(buffer)
            buffer.limit(limit)

            // Padding if needed
            repeat((0 until packet.remaining()).count()) { packet.put(0xFF.toByte()) }

            writePacket(packet)
        }

        return Error.SUCCESS
    }
}