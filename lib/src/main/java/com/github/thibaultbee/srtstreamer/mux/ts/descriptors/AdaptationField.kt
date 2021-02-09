package com.github.thibaultbee.srtstreamer.mux.ts.descriptors

import com.github.thibaultbee.srtstreamer.mux.ts.data.ITSElement
import com.github.thibaultbee.srtstreamer.mux.ts.utils.TSConst
import net.magik6k.bitbuffer.BitBuffer
import java.nio.ByteBuffer
import kotlin.math.pow

class AdaptationField(
    private val discontinuityIndicator: Boolean = false,
    private val randomAccessIndicator: Boolean = false,
    private val elementaryStreamPriorityIndicator: Boolean = false,
    private val programClockReference: Long? = null,
    private val originalProgramClockReference: Long? = null,
    private val spliceCountdown: Byte? = null,
    private val transportPrivateData: ByteBuffer? = null,
    private val adaptationFieldExtension: ByteBuffer? = null
) : ITSElement {
    override val bitSize = computeBitSize()
    override val size = bitSize / Byte.SIZE_BITS

    private val adaptationFieldLength = size - 1 // 1 - adaptationField own size

    private fun computeBitSize(): Int {
        var nBits = 16 // 16 - header
        programClockReference?.let { nBits += 48 }
        originalProgramClockReference?.let { nBits += 48 }
        spliceCountdown?.let { nBits += 8 }
        transportPrivateData?.let { nBits += transportPrivateData.remaining() }
        adaptationFieldExtension?.let { nBits += adaptationFieldExtension.remaining() }

        return nBits
    }

    override fun asByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())
        buffer.put(adaptationFieldLength, 8)
        buffer.put(discontinuityIndicator)
        buffer.put(randomAccessIndicator)
        buffer.put(elementaryStreamPriorityIndicator)
        programClockReference?.let { buffer.put(true) } ?: buffer.put(false)
        originalProgramClockReference?.let { buffer.put(true) } ?: buffer.put(false)
        spliceCountdown?.let { buffer.put(true) } ?: buffer.put(false)
        transportPrivateData?.let { buffer.put(true) } ?: buffer.put(false)
        adaptationFieldExtension?.let { buffer.put(true) } ?: buffer.put(false)

        programClockReference?.let {
            addClockReference(buffer, it)
        }
        originalProgramClockReference?.let {
            addClockReference(buffer, it)
        }
        spliceCountdown?.let {
            buffer.put(spliceCountdown)
            NotImplementedError("spliceCountdown not implemented yet")
        }
        transportPrivateData?.let {
            buffer.put(it.remaining())
            buffer.put(transportPrivateData)
            NotImplementedError("transportPrivateData not implemented yet")
        }
        adaptationFieldExtension?.let {
            NotImplementedError("adaptationFieldExtension not implemented yet")
        }

        return buffer.asByteBuffer()
    }

    private fun addClockReference(buffer: BitBuffer, timestamp: Long) {
        val pcrBase =
            (TSConst.SYSTEM_CLOCK_FREQ * timestamp / 1000000 /* µs -> s */ / 300) % 2.toDouble()
                .pow(33)
                .toLong()
        val pcrExt = (TSConst.SYSTEM_CLOCK_FREQ * timestamp / 1000000 /* µs -> s */) % 300

        buffer.put(pcrBase, 33)
        buffer.put(0b111111, 6) // Reserved
        buffer.put(pcrExt, 9)
    }
}
