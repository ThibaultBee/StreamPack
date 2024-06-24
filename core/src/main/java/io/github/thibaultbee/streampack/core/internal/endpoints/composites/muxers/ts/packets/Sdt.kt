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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.packets

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.ITSElement
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.Service
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putString
import java.nio.ByteBuffer


class Sdt(
    listener: io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxer.IMuxerListener? = null,
    private val services: List<Service>,
    tsId: Short,
    private val originalNetworkId: Short = 0xff01.toShort(),
    versionNumber: Byte = 0,
    var packetCount: Int = 0,
) : Psi(
    listener,
    PID,
    TID,
    true,
    true,
    tsId,
    versionNumber = versionNumber
),
    ITSElement {
    companion object {
        // Table ids
        const val TID: Byte = 0x42

        // Pids
        const val PID: Short = 0x0011
    }

    override val bitSize: Int
        get() = computeBitSize()
    override val size: Int
        get() = bitSize / Byte.SIZE_BITS

    private fun computeBitSize(): Int {
        var nBits = 24
        nBits += services.map { 80 + (it.info.providerName.length + it.info.name.length) * Byte.SIZE_BITS }
            .sum()

        return nBits
    }

    fun write() {
        if (services.isNotEmpty()) {
            write(toByteBuffer())
        }
    }

    override fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)

        buffer.putShort(originalNetworkId)
        buffer.put(0b11111111) // Reserved for future use

        services.map { it.info }.forEach {
            buffer.putShort(it.id)
            buffer.put(0b11111100) // Reserved + EIT_schedule_flag + EIT_present_following_flag

            val serviceDescriptorLength = 3 + it.providerName.length + it.name.length
            val descriptorsLoopLength =
                2 + serviceDescriptorLength // 2 = descriptor_tag + descriptor_length
            buffer.putShort(
                (0b1000 shl 12) // running_status - 4 -> running + free_CA_mode
                        or (descriptorsLoopLength)
            )

            // Service descriptor
            buffer.put(0x48) // descriptor_tag
            buffer.put(serviceDescriptorLength)

            buffer.put(it.type.value)

            buffer.put(it.providerName.length)
            buffer.putString(it.providerName)

            buffer.put(it.name.length)
            buffer.putString(it.name)
        }

        buffer.rewind()
        return buffer
    }

}