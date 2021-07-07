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
package com.github.thibaultbee.streampack.internal.muxers.ts.packets

import com.github.thibaultbee.streampack.internal.bitbuffer.BitBuffer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ITSElement
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

