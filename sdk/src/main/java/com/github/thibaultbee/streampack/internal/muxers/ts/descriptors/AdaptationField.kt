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
package com.github.thibaultbee.streampack.internal.muxers.ts.descriptors

import com.github.thibaultbee.streampack.internal.bitbuffer.BitBuffer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ITSElement
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.TSConst
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

    override fun toByteBuffer(): ByteBuffer {
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

        return buffer.toByteBuffer()
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
