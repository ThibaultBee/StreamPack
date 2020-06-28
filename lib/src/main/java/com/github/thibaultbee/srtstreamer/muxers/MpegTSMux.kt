package com.github.thibaultbee.srtstreamer.muxers

import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.models.Frame
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer
import kotlin.experimental.or

class MpegTSMux(logger: Logger, muxerListener: MuxerListener? = null) :
    MpegTSBase(logger, muxerListener) {
    private var services = mutableListOf<MpegTSService>()
    private var streams = mutableListOf<MpegTSStream>()
    private lateinit var pat: MpegTSPat
    private lateinit var sdt: MpegTSSdt
    private lateinit var pmt: MpegTSPmt

    private val patPacketPeriod = 40
    private var patPacketCount = patPacketPeriod - 1// Output PAT ASAP

    private val sdtPacketPeriod = 200
    private var sdtPacketCount = sdtPacketPeriod - 1// Output SDT ASAP

    fun writeFrame(frame: Frame): Error {
        when (frame.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                if (frame.isKeyFrame) {
                    if (frame.extra == null) {
                        logger.e(this, "Failed to get extra for AVC")
                    }
                    val buffer =
                        ByteBuffer.allocate(6 + (frame.extra?.limit() ?: 0) + frame.buffer.limit())
                    buffer.putInt(0x00000001)
                    buffer.put(0x09.toByte())
                    buffer.put(0xf0.toByte())
                    frame.extra?.let { buffer.put(it) }
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
                val buffer =
                    ByteBuffer.allocateDirect(frame.buffer.limit() + (frame.extra?.limit() ?: 0))
                frame.extra?.let { buffer.put(it) }
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

        pmt = MpegTSPmt(logger, muxerListener!!, 0x16)
        val service = MpegTSService(
            pmt,
            MpegTSService.ServiceType.DIGITAL_TV,
            0x4698,
            "Service01",
            "AndroidSRTStreamer",
            pcrPid
        )
        services.add(service)

        pat = MpegTSPat(logger, muxerListener!!)
        sdt = MpegTSSdt(logger, muxerListener!!)

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
            sdt.write(services)
        }

        patPacketCount += 1
        if ((patPacketCount == patPacketPeriod) || forcePat) {
            patPacketCount = 0
            pat.write(services)
            services.forEach { pmt.write(it, streams) }
        }

        return Error.SUCCESS
    }
}