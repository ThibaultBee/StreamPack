package io.github.thibaultbee.streampack.internal.muxers.ts.utils.av

import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.shl
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.math.min

data class OpusControlHeader(
    private val prefix: Short = 0x3FF,
    private val payloadSize: Int,
    private val startTrim: Short? = null,
    private val endTrim: Short? = null,
) : ByteBufferWriter() {
    override val size =
        3 + payloadSize / UByte.MAX_VALUE.toInt() + (startTrim?.let { 2 }
            ?: 0) + (endTrim?.let { 2 } ?: 0)

    override fun write(output: ByteBuffer) {
        // control_header_prefix 11 bits (0x3FF or 01111111111)
        // start_trim_flag 1 bit (0)
        // end_trim_flag 1 bit (0)
        // control_extension_flag 1 bit (0)
        // reserved 2 bits (0)
        output.putShort(((prefix shl 5)
                or ((startTrim?.let { 1 } ?: 0) shl 4)
                or ((endTrim?.let { 1 } ?: 0) shl 3)).toShort())

        var n = payloadSize
        while (n >= 0) {
            output.put(min(n, UByte.MAX_VALUE.toInt()))
            n -= UByte.MAX_VALUE.toInt()
        }

        if (startTrim != null) {
            output.putShort(startTrim and 0x7FF)
        }

        if (endTrim != null) {
            output.putShort(endTrim and 0x7FF)
        }
    }
}