package com.github.thibaultbee.streampack.muxers.ts.packets

import com.github.thibaultbee.streampack.data.Frame
import com.github.thibaultbee.streampack.muxers.IMuxerListener
import com.github.thibaultbee.streampack.muxers.ts.data.Stream
import com.github.thibaultbee.streampack.muxers.ts.descriptors.AdaptationField
import com.github.thibaultbee.streampack.muxers.ts.packets.Pes.StreamId.Companion.fromMimeType
import com.github.thibaultbee.streampack.utils.isAudio
import com.github.thibaultbee.streampack.utils.isVideo

class Pes(
    muxerListener: IMuxerListener,
    val stream: Stream,
    private val hasPcr: Boolean,
) : TS(muxerListener, stream.pid) {
    fun write(frame: Frame) {
        val programClockReference = if (hasPcr) {
            frame.pts - 300
        } else {
            null
        }
        val adaptationField = AdaptationField(
            discontinuityIndicator = stream.discontinuity,
            randomAccessIndicator = frame.isKeyFrame,
            programClockReference = programClockReference
        )

        val header = PesHeader(
            streamId = fromMimeType(stream.mimeType).value,
            payloadLength = frame.buffer.remaining().toShort(),
            pts = frame.pts,
            dts = frame.dts
        )


        write(frame.buffer, adaptationField, header.toByteBuffer(), true)
    }

    enum class StreamId(val value: Short) {
        PRIVATE_STREAM_1(0xbd.toShort()),
        AUDIO_STREAM_0(0xc0.toShort()),
        VIDEO_STREAM_0(0xe0.toShort()),
        METADATA_STREAM(0xfc.toShort()),
        EXTENDED_STREAM(0xfd.toShort());

        companion object {
            fun fromMimeType(mimeType: String): StreamId {
                return when {
                    mimeType.isVideo() -> {
                        VIDEO_STREAM_0
                    }
                    mimeType.isAudio() -> {
                        AUDIO_STREAM_0
                    }
                    else -> {
                        METADATA_STREAM
                    }
                }
            }
        }
    }
}