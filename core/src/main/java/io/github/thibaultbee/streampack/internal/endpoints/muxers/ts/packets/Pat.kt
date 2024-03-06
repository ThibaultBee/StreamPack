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

import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.ITSElement
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.Service
import java.nio.ByteBuffer
import kotlin.experimental.or

class Pat(
    listener: IMuxer.IMuxerListener? = null,
    private val services: List<Service>,
    tsId: Short,
    versionNumber: Byte = 0,
    var packetCount: Int = 0,
) : Psi(
    listener,
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
        val buffer = ByteBuffer.allocate(size)

        services
            .filter { it.pmt != null }
            .forEach {
                buffer.putShort(it.info.id)
                buffer.putShort(
                    (0b111 shl 13).toShort()  // reserved
                            or it.pmt!!.pid
                )
            }

        buffer.rewind()
        return buffer
    }
}