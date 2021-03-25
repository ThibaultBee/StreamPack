package com.github.thibaultbee.streampack.muxers.ts.tables

import com.github.thibaultbee.streampack.muxers.ts.data.ITSElement
import com.github.thibaultbee.streampack.utils.BitBuffer
import java.nio.ByteBuffer

class TableHeader(
    private val tableId: Byte,
    private val sectionSyntaxIndicator: Boolean,
    private val reservedFutureUse: Boolean = false,
    payloadLength: Short,
    private val tableIdExtension: Short = 0,
    private val versionNumber: Byte,
    private val currentNextIndicator: Boolean = true,
    private val sectionNumber: Byte,
    private val lastSectionNumber: Byte,
) : ITSElement {
    override val bitSize = 64
    override val size = bitSize / Byte.SIZE_BITS

    private val sectionLength = payloadLength + 5 + Psi.CRC_SIZE // 5 - header

    override fun toByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())
        buffer.put(tableId, 8)
        buffer.put(sectionSyntaxIndicator)
        buffer.put(reservedFutureUse) // Set to 1 for SDT
        buffer.put(0b11, 2)
        buffer.put(0b00, 2)
        buffer.put(sectionLength, 10)
        buffer.put(tableIdExtension, 16)
        buffer.put(0b11, 2)
        buffer.put(versionNumber, 5)
        buffer.put(currentNextIndicator)
        buffer.put(sectionNumber, 8)
        buffer.put(lastSectionNumber, 8)

        return buffer.toByteBuffer()
    }
}

