package com.github.thibaultbee.srtstreamer.muxers

import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.interfaces.MuxerListener
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.nio.ByteBuffer

class MpegTSPmt(
    logger: Logger,
    muxerListener: MuxerListener,
    val tablesVersion: Int = 0,
    val tsId: Int = 1
) : MpegTSSection(logger, muxerListener, PMT_TID) {
    fun write(service: MpegTSService, streams: List<MpegTSStream>): Error {
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
        writeSection1(buffer, PMT_TID, tsId, tablesVersion, 0, 0)

        return Error.SUCCESS
    }


    enum class StreamType(val value: Int) {
        VIDEO_MPEG1(0x01),
        VIDEO_MPEG2(0x02),
        AUDIO_MPEG1(0x03),
        AUDIO_MPEG2(0x04),
        PRIVATE_SECTION(0x05),
        PRIVATE_DATA(0x06),
        AUDIO_AAC(0x0f),
        AUDIO_AAC_LATM(0x11),
        VIDEO_MPEG4(0x10),
        METADATA(0x15),
        VIDEO_H264(0x1b),
        VIDEO_HEVC(0x24),
        VIDEO_CAVS(0x42),
        VIDEO_VC1(0xea),
        VIDEO_DIRAC(0xd1),

        AUDIO_AC3(0x81),
        AUDIO_DTS(0x82),
        AUDIO_TRUEHD(0x83),
        AUDIO_EAC3(0x87);

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