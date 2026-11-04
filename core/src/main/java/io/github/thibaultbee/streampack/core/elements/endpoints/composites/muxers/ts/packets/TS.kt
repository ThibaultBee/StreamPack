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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.packets

import io.github.thibaultbee.streampack.core.elements.endpoints.composites.data.SrtPacket
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxerInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.utils.MuxerConst
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.utils.TSOutputCallback
import io.github.thibaultbee.streampack.core.elements.utils.extensions.toInt
import io.github.thibaultbee.streampack.core.elements.utils.pool.ByteBufferPool
import java.nio.ByteBuffer
import java.security.InvalidParameterException

open class TS(
    private val byteBufferPool: ByteBufferPool,
    listener: IMuxerInternal.IMuxerListener? = null,
    val pid: Short,
    private val transportErrorIndicator: Boolean = false,
    private val transportPriority: Boolean = false,
    private val transportScramblingControl: Byte = 0, // Not scrambled
    private var continuityCounter: Byte = 0,
) : TSOutputCallback(listener) {

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

        val buffer = byteBufferPool.get(PACKET_SIZE * MuxerConst.MAX_OUTPUT_PACKET_NUMBER)

        try {
            while (payload?.hasRemaining() == true || adaptationFieldIndicator) {
                val packetPosition = buffer.position()
                buffer.limit(packetPosition + PACKET_SIZE)

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

                val stuffingSize = if (stuffingForLastPacket && payload != null) {
                    buffer.remaining() - payload.remaining() - (specificHeader?.remaining() ?: 0) -
                            if (adaptationFieldIndicator) adaptationField!!.remaining() else 0
                } else {
                    0
                }

                // Add adaptation fields first if needed. Stuffing goes at its end.
                if (adaptationFieldIndicator) {
                    val adaptationFieldPosition = buffer.position()
                    buffer.put(adaptationField!!) // Is not null if adaptationFieldIndicator is true
                    adaptationFieldIndicator = false
                    if (stuffingSize > 0) {
                        val adaptationFieldLength = buffer[adaptationFieldPosition].toInt() and 0xFF
                        var stuffingBytes = stuffingSize
                        if (adaptationFieldLength == 0) {
                            buffer.put(0.toByte()) // Flags
                            stuffingBytes--
                        }
                        repeat(stuffingBytes) {
                            buffer.put(0xFF.toByte()) // Stuffing
                        }
                        buffer.put(adaptationFieldPosition, (adaptationFieldLength + stuffingSize).toByte())
                    }
                } else if (stuffingSize > 0) {
                    byte = buffer[packetPosition + 3].toInt() or (1 shl 5)
                    buffer.put(packetPosition + 3, byte.toByte())
                    buffer.put((stuffingSize - 1).toByte())
                    if (stuffingSize > 1) {
                        buffer.put(0.toByte()) // Flags
                        repeat(stuffingSize - 2) {
                            buffer.put(0xFF.toByte()) // Stuffing
                        }
                    }
                }

                // Then specific stream header. Mainly for PES header.
                specificHeader?.let {
                    buffer.put(it)
                }

                // Fill packet with correct size of payload
                payload?.let {
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
                        SrtPacket(
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
        } finally {
            byteBufferPool.put(buffer)
        }
    }
}