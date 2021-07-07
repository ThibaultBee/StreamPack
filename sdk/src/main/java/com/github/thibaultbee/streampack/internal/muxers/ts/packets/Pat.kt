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
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ITSElement
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Service
import java.nio.ByteBuffer

class Pat(
    muxerListener: IMuxerListener,
    private val services: List<Service>,
    tsId: Short,
    versionNumber: Byte = 0,
    var packetCount: Int = 0,
) : Psi(
    muxerListener,
    PID,
    TID,
    true,
    false,
    tsId,
    versionNumber,
), ITSElement {
    companion object {
        // Table ids
        const val TID: Byte = 0x00

        // Pids
        const val PID: Short = 0x0000
    }

    override val bitSize: Int
        get() = 32 * services.filter { it.pmt != null }.size
    override val size: Int
        get() = bitSize / Byte.SIZE_BITS

    fun write() {
        if (services.any { it.pmt != null }) {
            write(toByteBuffer())
        }
    }

    override fun toByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())

        services
            .filter { it.pmt != null }
            .forEach {
                buffer.put(it.info.id)
                buffer.put(0b111, 3)  // reserved
                buffer.put(it.pmt!!.pid, 13)
            }

        return buffer.toByteBuffer()
    }
}