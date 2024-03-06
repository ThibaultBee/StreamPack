/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.packets

import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.ITSElement
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.utils.TSConst
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.internal.utils.extensions.toInt
import java.nio.ByteBuffer
import kotlin.experimental.and
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

    override fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)

        buffer.putShort(0) // start code is 0x000001
        buffer.put(1)
        buffer.put(streamId)
        if (pesPacketLength > 0xFFFF)
            pesPacketLength = 0
        buffer.putShort(pesPacketLength)
        // Optional
        buffer.put(
            ((0b10 shl 6)
                    or ((esScramblingControl and 0x3) shl 4)
                    or (esPriority shl 3)
                    or (dataAlignmentIndicator shl 2)
                    or (copyright shl 1)
                    or (originalOrCopy.toInt())
                    )
        )

        // 7 flags
        buffer.put((((pts?.let { 1 } ?: 0) shl 7)
                or ((dts?.let { 1 } ?: 0) shl 6)
                or ((esClockReference?.let { 1 } ?: 0) shl 5)
                or ((esRate?.let { 1 } ?: 0) shl 4)
                or ((dsmTrickMode?.let { 1 } ?: 0) shl 3)
                or ((additionalCopyInfo?.let { 1 } ?: 0) shl 2)
                or ((crc?.let { 1 } ?: 0) shl 1)
                or 0 // PES_extension_flag
                ))

        esClockReference?.let {
            NotImplementedError("esClockReference not implemented yet")
        }
        esRate?.let {
            NotImplementedError("esRate not implemented yet")
        }
        dsmTrickMode?.let {
            NotImplementedError("dsmTrickMode not implemented yet")
        }
        additionalCopyInfo?.let {
            NotImplementedError("additionalCopyInfo not implemented yet")
        }
        crc?.let {
            NotImplementedError("crc not implemented yet")
        }

        buffer.put(pesHeaderDataLength.toByte())
        pts?.let {
            addTimestamp(buffer, it, dts?.let { 0b0011 } ?: 0b0010)
        }
        dts?.let {
            addTimestamp(buffer, it, 0b1)
        }

        buffer.rewind()
        return buffer
    }

    private fun addTimestamp(buffer: ByteBuffer, timestamp: Long, fourBits: Byte) {
        val pts =
            (TSConst.SYSTEM_CLOCK_FREQ * timestamp / 1000000 /* µs -> s */ / 300) % 2.toDouble()
                .pow(33)
                .toLong()

        buffer.put(
            (((fourBits and 0xF).toInt() shl 4)
                    or ((pts shr 29) and 0xE).toInt()
                    or 1)
        )
        buffer.putShort(
            ((pts shr 14) and 0xFFFE)
                    or 1
        )
        buffer.putShort(
            ((pts shl 1) and 0xFFFE)
                    or 1
        )
    }
}
