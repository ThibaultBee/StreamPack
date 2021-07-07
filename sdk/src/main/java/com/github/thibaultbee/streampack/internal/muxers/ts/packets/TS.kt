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

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.MuxerConst
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.TSOutputCallback
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.toInt
import java.nio.ByteBuffer
import java.security.InvalidParameterException

open class TS(
    muxerListener: IMuxerListener,
    val pid: Short,
    private val transportErrorIndicator: Boolean = false,
    private val transportPriority: Boolean = false,
    private val transportScramblingControl: Byte = 0, // Not scrambled
    private var continuityCounter: Byte = 0,
) : TSOutputCallback(muxerListener) {

    companion object {
        const val SYNC_BYTE: Byte = 0x47
        const val PACKET_SIZE = 188
    }

    protected fun write(
        payload: ByteBuffer? = null,
        adaptationField: ByteBuffer? = null,
        specificHeader: ByteBuffer? = null,
        stuffingForLastPacket: Boolean = false,
        timestamp: Long = 0L
    ) {
        val payloadLimit = payload?.limit() ?: 0
        var payloadUnitStartIndicator = true

        var adaptationFieldIndicator = adaptationField != null
        val payloadIndicator = payload != null

        var packetIndicator = 0

        val buffer = ByteBuffer.allocateDirect(PACKET_SIZE * MuxerConst.MAX_OUTPUT_PACKET_NUMBER)

        while (payload?.hasRemaining() == true || adaptationFieldIndicator) {
            buffer.limit(buffer.position() + PACKET_SIZE)

            // Write header to packet
            buffer.put(SYNC_BYTE)
            var byte =
                (transportErrorIndicator.toInt() shl 7) or (payloadUnitStartIndicator.toInt() shl 6) or (transportPriority.toInt() shl 5) or (pid.toInt() shr 8)
            buffer.put(byte.toByte())
            payloadUnitStartIndicator = false
            buffer.put(pid.toByte())
            byte =
                (transportScramblingControl.toInt() shl 6) or (continuityCounter.toInt() and 0xF) or
                        when {
                            adaptationFieldIndicator and payloadIndicator -> {
                                (0b11 shl 4)
                            }
                            adaptationFieldIndicator -> {
                                (0b10 shl 4)
                            }
                            payloadIndicator -> {
                                (0b01 shl 4)
                            }
                            else -> throw InvalidParameterException("TS must have either a payload either an adaption field")
                        }
            buffer.put(byte.toByte())
            continuityCounter = ((continuityCounter + 1) and 0xF).toByte()

            // Add adaptation fields first if needed
            if (adaptationFieldIndicator) {
                buffer.put(adaptationField!!) // Is not null if adaptationFieldIndicator is true
                adaptationFieldIndicator = false
            }

            // Then specific stream header. Mainly for PES header.
            specificHeader?.let {
                buffer.put(it)
            }

            // Fill packet with correct size of payload
            payload?.let {
                if (stuffingForLastPacket) {
                    // Add stuffing before last packet remaining payload
                    if (buffer.remaining() > it.remaining()) {
                        val headerSize = buffer.position() % PACKET_SIZE
                        val currentPacketFirstPosition =
                            buffer.position() / PACKET_SIZE * PACKET_SIZE
                        byte = buffer[currentPacketFirstPosition + 3].toInt()
                        byte = byte or (1 shl 5)
                        buffer.position(currentPacketFirstPosition + 3)
                        buffer.put(byte.toByte())
                        buffer.position(currentPacketFirstPosition + 4)
                        val stuffingLength = PACKET_SIZE - it.remaining() - headerSize - 1
                        buffer.put(stuffingLength.toByte())
                        if (stuffingLength >= 1) {
                            buffer.put(0.toByte())
                            for (i in 0..stuffingLength - 2) {
                                buffer.put(0xFF.toByte()) // Stuffing
                            }
                        }
                    }
                }

                it.limit(it.position() + buffer.remaining().coerceAtMost(it.remaining()))
                buffer.put(it)
                it.limit(payloadLimit)
            }

            while (buffer.hasRemaining()) {
                buffer.put(0xFF.toByte())
            }

            val isLastPacket = payload?.let { !it.hasRemaining() } ?: true
            if (buffer.limit() == buffer.capacity() || isLastPacket) {
                writePacket(
                    Packet(
                        buffer,
                        packetIndicator == 0,
                        isLastPacket,
                        timestamp
                    )
                )
                buffer.rewind()
            }

            packetIndicator++
        }
    }
}