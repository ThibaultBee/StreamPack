package com.github.thibaultbee.srtstreamer.mux.ts.packets

import com.github.thibaultbee.srtstreamer.mux.ts.data.ITSElement
import com.github.thibaultbee.srtstreamer.mux.ts.utils.TSConst
import net.magik6k.bitbuffer.BitBuffer
import java.nio.ByteBuffer
import kotlin.math.pow

class PesHeader(
    private val streamId: Short,
    payloadLength: Short,
    private val esScramblingControl: Byte = 0,
    private val esPriority: Boolean = false,
    private val dataAlignmentIndicator: Boolean = false,
    private val copyright: Boolean = false,
    private val originalOrCopy: Boolean = true,
    private val pts: Long? = null,
    private val dts: Long? = null,
    private val esClockReference: Long? = null,
    private val esRate: Int? = null,
    private val dsmTrickMode: Byte? = null,
    private val additionalCopyInfo: Byte? = null,
    private val crc: Short? = null
) : ITSElement {
    init {
        dts?.let { requireNotNull(pts) { "If dts is not null, pts must be not null " } }
    }

    override val bitSize: Int
        get() = 72 + pesHeaderDataBitLength  // 24 - start code / 8 - stream ids / 16 - packet length / 24 - optional PES header
    override val size = bitSize / Byte.SIZE_BITS

    private var pesPacketLength = payloadLength + pesHeaderDataLength + 3

    private val pesHeaderDataBitLength: Int
        get() {
            var nBits = 0
            pts?.let { nBits += 40 }
            dts?.let { nBits += 40 }
            esClockReference?.let { nBits += 42 }
            esRate?.let { nBits += 22 }
            dsmTrickMode?.let { nBits += 8 }
            additionalCopyInfo?.let { nBits += 7 }
            crc?.let { nBits += 16 }
            return nBits
        }
    private val pesHeaderDataLength: Int
        get() = pesHeaderDataBitLength / Byte.SIZE_BITS

    override fun asByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())
        buffer.put(1, 24)
        buffer.put(streamId, 8)
        if (pesPacketLength > 0xFFFF)
            pesPacketLength = 0
        buffer.put(pesPacketLength, 16)
        // Optional
        buffer.put(0b10, 2)
        buffer.put(esScramblingControl, 2)
        buffer.put(esPriority)
        buffer.put(dataAlignmentIndicator)
        buffer.put(copyright)
        buffer.put(originalOrCopy)

        // 7 flags
        pts?.let {
            buffer.put(true)
        } ?: buffer.put(false)
        dts?.let {
            buffer.put(true)
        } ?: buffer.put(false)
        esClockReference?.let {
            buffer.put(true)
            NotImplementedError("esClockReference not implemented yet")
        } ?: buffer.put(false)
        esRate?.let {
            buffer.put(true)
            NotImplementedError("esRate not implemented yet")
        } ?: buffer.put(false)
        dsmTrickMode?.let {
            buffer.put(true)
            NotImplementedError("dsmTrickMode not implemented yet")
        } ?: buffer.put(false)
        additionalCopyInfo?.let {
            buffer.put(true)
            NotImplementedError("additionalCopyInfo not implemented yet")
        } ?: buffer.put(false)
        crc?.let {
            buffer.put(true)
            NotImplementedError("crc not implemented yet")
        } ?: buffer.put(false)

        buffer.put(false) // PES_extension_flag
        buffer.put(pesHeaderDataLength, 8)
        pts?.let {
            addTimestamp(buffer, it, dts?.let { 0b0011 } ?: 0b0010)
        }
        dts?.let {
            addTimestamp(buffer, it, 0b1)
        }

        return buffer.asByteBuffer()
    }

    private fun addTimestamp(buffer: BitBuffer, timestamp: Long, fourBits: Byte) {
        val pts =
            (TSConst.SYSTEM_CLOCK_FREQ * timestamp / 1000000 /* µs -> s */ / 300) % 2.toDouble()
                .pow(33)
                .toLong()

        buffer.put(fourBits, 4)
        buffer.put(pts shr 30, 3)
        buffer.put(true)
        buffer.put(pts shr 15, 15)
        buffer.put(true)
        buffer.put(pts, 15)
        buffer.put(true)
    }
}
