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
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.internal.utils.extensions.toInt
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
        val buffer = ByteBuffer.allocate(size)

        buffer.put(tableId)
        buffer.put(
            (sectionSyntaxIndicator shl 7)
                    or (reservedFutureUse shl 6)
                    or (0b11 shl 4)
                    // or (0b00 shl 2)
                    or ((sectionLength shr 8) and 0x3)
        )
        buffer.put(sectionLength)
        buffer.putShort(tableIdExtension)
        buffer.put(
            (0b11 shl 6)
                    or (versionNumber shl 1)
                    or (currentNextIndicator.toInt())
        )
        buffer.put(sectionNumber)
        buffer.put(lastSectionNumber)

        buffer.rewind()
        return buffer
    }
}

