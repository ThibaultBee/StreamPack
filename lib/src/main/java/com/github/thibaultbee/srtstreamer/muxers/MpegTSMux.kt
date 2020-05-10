package com.github.thibaultbee.srtstreamer.muxers

import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.experimental.or

class MpegTSMux(val logger: Logger) {
    var muxerListener: MuxerListener? = null
    private var services = mutableListOf<MpegTSService>()
    private var streams = mutableListOf<MpegTSStream>()
    private lateinit var pat: MpegTSSection
    private lateinit var sdt: MpegTSSection
    private lateinit var pmt: MpegTSSection
    private var tsid = 0x1
    private var onid = 0x1

    private var serviceType = 1
    private var tablesVersion = 0

    private val patPacketPeriod = 40
    private var patPacketCount = patPacketPeriod - 1// Output PAT ASAP

    private val sdtPacketPeriod = 200
    private var sdtPacketCount = sdtPacketPeriod - 1// Output SDT ASAP

    companion object {
        const val SECTION_LENGTH = 1020
        const val TS_PACKET_SIZE = 188

        // Table ids
        const val PAT_TID = 0x00
        const val PMT_TID = 0x02
        const val M4OD_TID = 0x05
        const val SDT_TID = 0x42

        // Pids
        const val PAT_PID = 0x0000
        const val SDT_PID = 0x0011
    }

    fun writeFrame(frame: Frame): Error {
        when (frame.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                if (frame.isKeyFrame) {
                    if (frame.extra == null) {
                        logger.e(this, "Failed to get extra for AVC")
                    }

                    val buffer = ByteBuffer.allocateDirect(6 + frame.extra!!.limit() + frame.buffer.limit())
                    buffer.putInt(0x00000001)
                    buffer.put(0x09.toByte())
                    buffer.put(0xf0.toByte())
                    buffer.put(frame.extra)
                    buffer.put(frame.buffer)
                    buffer.rewind()
                    frame.buffer = buffer
                }
            }
            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                TODO("Not yet implemented")
            }
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                // Copy ADTS
                if (frame.extra == null) {
                    logger.e(this, "Failed to get ADTS")
                }
                val buffer = ByteBuffer.allocateDirect(frame.buffer.limit() + frame.extra!!.limit())
                buffer.put(frame.extra)
                buffer.put(frame.buffer)
                buffer.rewind()
                frame.buffer = buffer
            }
            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                TODO("Not yet implemented")
            }
        }

        val stream: MpegTSStream
        try {
            stream = getStreamById(frame.pid)
        } catch (e: NoSuchElementException) {
            logger.e(this, "Failed to find stream with id ${frame.pid}")
            return Error.INVALID_PARAMETER
        }

        // Set timestamps to 90 kHz time base
        frame.dts = frame.dts * 90000 / 1000000
        frame.pts = frame.pts * 90000 / 1000000

        return writePes(stream, frame)
    }

    fun addStream(mimeType: String): Int {
        val pid = 0x100 + streams.size
        streams.add(MpegTSStream(mimeType, pid))

        return pid
    }

    fun configure(): Error {
        if (streams.isEmpty()) {
            logger.e(this, "AddStream before calling configure()")
            return Error.INVALID_PARAMETER
        }
        val pcrPid = try {
            streams.first { it.mimeType.startsWith("video") }.pid
        } catch (e: NoSuchElementException) {
            streams[0].pid
        }

        pmt = MpegTSSection(0x16)
        val service = MpegTSService(pmt, 0x4698, "Service01", "AndroidSRTStreamer", pcrPid)
        services.add(service)

        pat = MpegTSSection(PAT_PID)
        sdt = MpegTSSection(SDT_PID)

        return Error.SUCCESS
    }

    fun stop(): Error {
        streams.clear()

        return Error.SUCCESS
    }

    private fun getStreamByType(mimeType: String): List<MpegTSStream> {
        return streams.filter { it.mimeType == mimeType }
    }

    private fun getStreamById(id: Int): MpegTSStream {
        return streams.first { it.pid == id }
    }

    private fun writePes(stream: MpegTSStream, frame: Frame): Error {
        val limit = frame.buffer.limit()
        val packet = ByteBuffer.allocate(TS_PACKET_SIZE)
        val forcePat = frame.mimeType.startsWith("video") and frame.isKeyFrame
        var writePcr = false

        while(frame.buffer.hasRemaining()) {
            retransmitSi(forcePat)

            packet.rewind()
            packet.put(0x47.toByte())
            var byte = stream.pid shr 8
            if (frame.buffer.position() == 0) { // start
                byte = byte or 0x40
            }
            packet.put(byte.toByte())
            packet.put(stream.pid.toByte())
            stream.cc = (stream.cc + 1) and 0xF
            packet.put((0x10 or stream.cc).toByte())

            if (stream.discontinuity != 0) {
                afFlag(packet, 0x80)
                packet.position(tsPayloadStart(packet))
                stream.discontinuity = 0
            }

            if (frame.isKeyFrame && (frame.buffer.position() == 0)) {
                afFlag(packet, 0x40)
                packet.position(tsPayloadStart(packet))
                if (stream.pid == services[0].pcrPid) {
                    writePcr = true
                }
            }

            if (writePcr) {
                afFlag(packet, 0x10)
                packet.position(tsPayloadStart(packet))
                val pcr = frame.dts * 300
                packet.put(4, (packet.get(4) + writePcr(packet, pcr)).toByte())
            }

            if (frame.buffer.position() == 0) { // start
                packet.put(0x00.toByte())
                packet.put(0x00.toByte())
                packet.put(0x01.toByte())

                when {
                    frame.mimeType.startsWith("video") -> {
                        packet.put(0xe0.toByte())
                    }
                    frame.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC -> {
                        packet.put(0xc0.toByte())
                    }
                    else -> {
                        packet.put(0xdb.toByte())
                    }
                }
                var headerLen = 0
                var flags = 0

                if (frame.pts != -1L) {
                    headerLen += 5
                    flags = flags or 0x80
                }
                if (frame.dts != -1L) {
                    headerLen += 5
                    flags = flags or 0x40
                }
                var len = frame.buffer.remaining() + headerLen + 3
                if (len < 0xFFFF) {
                    len = 0
                }
                packet.put((len shr 8).toByte())
                packet.put((len).toByte())

                packet.put(0x80.toByte())
                packet.put(flags.toByte())
                packet.put(headerLen.toByte())

                if (frame.pts != -1L) {
                    writePts(packet, flags shr 6, frame.pts)
                }
                if (frame.dts != -1L) {
                    writePts(packet, flags shr 6, frame.dts)
                }
            }

            if (packet.remaining() > frame.buffer.remaining()) {
                val stuffingLen = packet.remaining() - frame.buffer.remaining()
                // Move usefull part to ts packet end
                val duplicateBuffer = packet.duplicate()
                duplicateBuffer.position(4)
                duplicateBuffer.limit(packet.position())
                packet.position(4 + stuffingLen)
                packet.put(duplicateBuffer)

                packet.put(3, (packet.get(3).toInt() or 0x20).toByte())
                packet.put(4, (stuffingLen - 1).toByte())
                // Add stuffing
                if (stuffingLen >= 2) {
                    packet.put(5, 0)
                    (6..stuffingLen + 4).forEach { packet.put(it, 0xFF.toByte()) }
                }
            }

            // Copying data
            frame.buffer.limit(frame.buffer.position() + TS_PACKET_SIZE - packet.position() /* header length */)
            // Handle producer (encoder) stop when data are being processing here
            try {
                packet.put(frame.buffer)
            } catch (e: IllegalStateException) {
                logger.e(this, "${e.message}")
                return Error.INVALID_OPERATION
            }
            frame.buffer.limit(limit)

            writePacket(packet)
        }

        return Error.SUCCESS
    }

    private fun writePts(buffer: ByteBuffer, fourBits: Int, pts: Long) {
        var value = fourBits shl 4 or (((pts shr 30) and 0x7) shl 1).toInt() or 1
        buffer.put(value.toByte())
        value = (((pts shr 15) and 0x7FFF) shl 1).toInt() or 1
        buffer.put((value shr 8).toByte())
        buffer.put((value).toByte())
        value = (((pts) and 0x7FFF) shl 1).toInt() or 1
        buffer.put((value shr 8).toByte())
        buffer.put((value).toByte())
    }

    private fun tsPayloadStart(buffer: ByteBuffer): Int {
        return if ((buffer.get(3).toInt() and 0x20) != 0) {
            5 + buffer.get(4)
        } else {
            4
        }
    }

    private fun afFlag(buffer: ByteBuffer, flag: Int) {
        val thirdByte = buffer.get(3)
        if ((thirdByte.toInt() and 0x20) == 0) {
            buffer.put(3, thirdByte or 0x20)
            buffer.put(4, 1)
            buffer.put(5, 0)
        }
        buffer.put(5, (buffer.get(5).toInt() or flag).toByte())
    }

    private fun writePcr(buffer: ByteBuffer, pcr: Long): Int {
        val pcrLow = pcr % 300
        val pcrHigh = pcr / 300

        buffer.put((pcrHigh shr 25).toByte())
        buffer.put((pcrHigh shr 17).toByte())
        buffer.put((pcrHigh shr 9).toByte())
        buffer.put((pcrHigh shr 1).toByte())
        buffer.put((pcrHigh shl 7 or pcrLow shr 8 or 0x7E).toByte())
        buffer.put(pcrLow.toByte())

        return 6
    }

    private fun retransmitSi(forcePat: Boolean): Error {
        sdtPacketCount += 1
        if (sdtPacketCount == sdtPacketPeriod) {
            sdtPacketCount = 0
            writeSdt()
        }

        patPacketCount += 1
        if ((patPacketCount == patPacketPeriod) || forcePat) {
            patPacketCount = 0
            writePat()
            services.forEach { writePmt(it) }
        }

        return Error.SUCCESS
    }

    private fun writePat(): Error {
        val buffer = ByteBuffer.allocate(SECTION_LENGTH)

        services.forEach {
            buffer.putShort(it.sid.toShort())
            buffer.putShort((0xe000 or it.pmt.pid).toShort())
        }

        buffer.limit(buffer.position())
        buffer.rewind()
        writeSection1(pat, buffer, PAT_TID, tsid, tablesVersion, 0, 0)

        return Error.SUCCESS
    }

    private fun writePmt(service: MpegTSService): Error {
        val buffer = ByteBuffer.allocate(SECTION_LENGTH)

        buffer.putShort((0xe000 or service.pcrPid).toShort())

        val programInfoLengthPosition = buffer.position()
        buffer.position(buffer.position() + 2)

        // TODO: Program Info

        var short = 0xF000 or (buffer.position() - programInfoLengthPosition - 2)
        buffer.put(programInfoLengthPosition, (short shr 8).toByte())
        buffer.put(programInfoLengthPosition + 1, (short).toByte())

        streams.forEach {
            buffer.put(StreamType.fromMimeType(it.mimeType).value.toByte())
            buffer.putShort((0xe000 or it.pid).toShort())

            val descLenghtPosition = buffer.position()
            buffer.position(buffer.position() + 2)

            // TODO: Optional descriptors

            short = 0xF000 or (buffer.position() - descLenghtPosition - 2)
            buffer.put(descLenghtPosition, (short shr 8).toByte())
            buffer.put(descLenghtPosition + 1, short.toByte())
        }

        buffer.limit(buffer.position())
        buffer.rewind()
        writeSection1(services[0].pmt, buffer, PMT_TID, tsid, tablesVersion, 0, 0)

        return Error.SUCCESS
    }


    fun writeSdt(): Error {
        val buffer = ByteBuffer.allocate(SECTION_LENGTH)

        buffer.put(onid.toByte())
        buffer.put(0xFF.toByte())

        services.forEach {
            buffer.putShort(it.sid.toShort())
            buffer.put(0xfc.toByte())

            val descListLenPosition = buffer.position()
            buffer.position(buffer.position() + 2)

            // write only one descriptor for the service name and provider
            buffer.put(0x48.toByte())

            val descLenPosition = buffer.position()
            buffer.position(buffer.position() + 1)

            buffer.put(serviceType.toByte())

            buffer.put(it.providerName.length.toByte())
            buffer.put(it.providerName.toByteArray())

            buffer.put(it.name.length.toByte())
            buffer.put(it.name.toByteArray())

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
        writeSection1(sdt, buffer, SDT_TID, tsid, tablesVersion, 0, 0)

        return Error.SUCCESS
    }


    private fun writeSection1(section: MpegTSSection, buffer: ByteBuffer, tid: Int, id: Int, version: Int, secNum: Int, lastSecNum: Int): Error {
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

        return writeSection(section, sectionBuffer)
    }

    private fun writeSection(section: MpegTSSection, buffer: ByteBuffer): Error {
        val packet = ByteBuffer.allocateDirect(TS_PACKET_SIZE)
        val limit = buffer.limit()

        while(buffer.hasRemaining()) {
            packet.rewind()
            packet.put(0x47)
            var byte = section.pid shr 8
            if (buffer.position() == 0) { // first
                byte = byte or 0x40
            }
            packet.put(byte.toByte())
            packet.put(section.pid.toByte())

            section.cc = (section.cc + 1) and 0xF
            packet.put((0x10 or section.cc).toByte())

            if (section.discontinuity != 0) {
                packet.put(packet.position() -1, packet.get(packet.position() -1) or 0x20)
                packet.put(1.toByte())
                packet.put(0x80.toByte())
                section.discontinuity = 0
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

    private fun writePacket(buffer: ByteBuffer): Error {
        buffer.rewind()
        return muxerListener?.onOutputFrame(buffer) ?: Error.INVALID_OPERATION
    }


    data class MpegTSSection(val pid: Int, var cc: Int = 15, var discontinuity: Int = 0)
    data class MpegTSStream(val mimeType: String, val pid: Int, var cc: Int = 15, var discontinuity: Int = 0)
    data class MpegTSService(val pmt: MpegTSSection, val sid: Int, val name: String, val providerName: String, var pcrPid: Int = 0x1fff, var pcrPacketCount: Int = 0, var pcrPacketPeriod: Int = 0)

    enum class StreamType (val value: Int) {
        VIDEO_MPEG1 (0x01),
        VIDEO_MPEG2 (0x02),
        AUDIO_MPEG1 (0x03),
        AUDIO_MPEG2 (0x04),
        PRIVATE_SECTION (0x05),
        PRIVATE_DATA (0x06),
        AUDIO_AAC (0x0f),
        AUDIO_AAC_LATM (0x11),
        VIDEO_MPEG4 (0x10),
        METADATA (0x15),
        VIDEO_H264 (0x1b),
        VIDEO_HEVC (0x24),
        VIDEO_CAVS (0x42),
        VIDEO_VC1 (0xea),
        VIDEO_DIRAC (0xd1),

        AUDIO_AC3 (0x81),
        AUDIO_DTS (0x82),
        AUDIO_TRUEHD (0x83),
        AUDIO_EAC3 (0x87);

        companion object {
            fun fromMimeType(mimeType: String) = when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_MPEG2 -> VIDEO_MPEG2
                MediaFormat.MIMETYPE_AUDIO_MPEG -> AUDIO_MPEG1
                MediaFormat.MIMETYPE_AUDIO_AAC -> AUDIO_AAC
                MediaFormat.MIMETYPE_VIDEO_MPEG4 -> VIDEO_MPEG4
                MediaFormat.MIMETYPE_VIDEO_AVC -> VIDEO_H264
                MediaFormat.MIMETYPE_VIDEO_HEVC -> VIDEO_HEVC
                MediaFormat.MIMETYPE_AUDIO_AC3 -> AUDIO_AC3
                MediaFormat.MIMETYPE_AUDIO_EAC3 -> AUDIO_EAC3
                MediaFormat.MIMETYPE_AUDIO_OPUS -> PRIVATE_DATA
                else -> PRIVATE_DATA
            }
        }
    }
}