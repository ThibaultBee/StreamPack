package io.github.thibaultbee.streampack.internal.utils.av.audio.opus

import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import java.nio.ByteBuffer

class OpusFrameWriter(private val frameBuffer: ByteBuffer) : ByteBufferWriter() {
    private val payloadSize: Int
        get() = frameBuffer.remaining()
    private val payloadSizeFullBytesCount: Int
        get() = payloadSize / 255
    private val payloadSizeRemainderByte: Int
        get() = payloadSize % 255
    private val controlHeaderSize: Int
        get() = 2 + payloadSizeFullBytesCount + payloadSizeRemainderByte.coerceAtMost(1)

    override val size = controlHeaderSize + payloadSize

    override fun write(output: ByteBuffer) {
        // control_header_prefix 11 bits (0x3FF or 01111111111)
        // start_trim_flag 1 bit (0)
        // end_trim_flag 1 bit (0)
        // control_extension_flag 1 bit (0)
        // reserved 2 bits (0)
        output.put(0x7F.toByte())
        output.put(0xE0.toByte())

        repeat(payloadSizeFullBytesCount) {
            output.put(0xFF.toByte())
        }
        if (payloadSizeRemainderByte > 0) {
            output.put(payloadSizeRemainderByte.toByte())
        }

        output.put(frameBuffer)
    }

    companion object {
        fun fromPayload(frameBuffer: ByteBuffer): OpusFrameWriter {
            return OpusFrameWriter(frameBuffer)
        }
    }
}