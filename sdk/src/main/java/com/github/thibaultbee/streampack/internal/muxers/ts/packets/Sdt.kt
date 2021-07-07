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


class Sdt(
    muxerListener: IMuxerListener,
    private val services: List<Service>,
    tsId: Short,
    private val originalNetworkId: Short = 0xff01.toShort(),
    versionNumber: Byte = 0,
    var packetCount: Int = 0,
) : Psi(
    muxerListener,
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
        val buffer = BitBuffer.allocate(bitSize.toLong())

        buffer.put(originalNetworkId, 16)
        buffer.put(0b11111111, 8) // Reserved for future use

        services.map { it.info }.forEach {
            buffer.put(it.id, 16)
            buffer.put(0b111111, 6) // Reserved

            buffer.put(0, 1) // EIT_schedule_flag
            buffer.put(0, 1) // EIT_present_following_flag

            buffer.put(4, 3) // running_status - 4 -> running
            buffer.put(0, 1) // free_CA_mode

            val serviceDescriptorLength = 3 + it.providerName.length + it.name.length
            val descriptorsLoopLength =
                2 + serviceDescriptorLength // 2 = descriptor_tag + descriptor_length
            buffer.put(descriptorsLoopLength, 12)

            // Service descriptor
            buffer.put(0x48, 8) // descriptor_tag
            buffer.put(serviceDescriptorLength, 8)

            buffer.put(it.type.value, 8)

            buffer.put(it.providerName.length, 8)
            buffer.put(it.providerName)

            buffer.put(it.name.length, 8)
            buffer.put(it.name)
        }

        return buffer.toByteBuffer()
    }

}