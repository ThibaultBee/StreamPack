package com.github.thibaultbee.streampack.muxers.ts.packets

import com.github.thibaultbee.streampack.muxers.IMuxerListener
import com.github.thibaultbee.streampack.muxers.ts.descriptors.AdaptationField
import com.github.thibaultbee.streampack.muxers.ts.utils.TSOutputCallback
import com.github.thibaultbee.streampack.utils.toInt
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
        adaptationField: AdaptationField? = null,
        specificHeader: ByteBuffer? = null,
        stuffingForLastPacket: Boolean = false
    ) {
        val payloadLimit = payload?.limit() ?: 0
        var payloadUnitStartIndicator = true

        var adaptationFieldIndicator = adaptationField != null
        val payloadIndicator = payload != null

        while (payload?.hasRemaining() == true || adaptationFieldIndicator) {
            val packet = ByteBuffer.allocate(PACKET_SIZE)

            // Write header to packet
            packet.put(SYNC_BYTE)
            var byte =
                (transportErrorIndicator.toInt() shl 7) or (payloadUnitStartIndicator.toInt() shl 6) or (transportPriority.toInt() shl 5) or (pid.toInt() shr 8)
            packet.put(byte.toByte())
            payloadUnitStartIndicator = false
            packet.put(pid.toByte())
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
            packet.put(byte.toByte())
            continuityCounter = ((continuityCounter + 1) and 0xF).toByte()

            // Add adaptation fields first if needed
            if (adaptationFieldIndicator) {
                packet.put(adaptationField!!.toByteBuffer()) // Is not null if adaptationFieldIndicator is true
                adaptationFieldIndicator = false
            }

            // Then specific stream header. Mainly for PES header.
            specificHeader?.let {
                packet.put(it)
            }

            // Fill packet with correct size of payload
            payload?.let {
                if (stuffingForLastPacket) {
                    // Add stuffing before last packet remaining payload
                    if (packet.remaining() > it.remaining()) {
                        val headerLength = packet.position()
                        byte = packet[3].toInt()
                        byte = byte or (1 shl 5)
                        packet.position(3)
                        packet.put(byte.toByte())
                        packet.position(4)
                        val stuffingLength = PACKET_SIZE - it.remaining() - headerLength - 1
                        packet.put(stuffingLength.toByte())
                        if (stuffingLength >= 2) {
                            packet.put(0.toByte())
                            while (packet.position() < (PACKET_SIZE - it.remaining()).toLong()) {
                                packet.put(0xFF.toByte()) // Stuffing
                            }
                        }
                    }
                }

                it.limit(it.position() + packet.remaining().coerceAtMost(it.remaining()))
                packet.put(it)
                it.limit(payloadLimit)
            }

            while (packet.hasRemaining()) {
                packet.put(0xFF.toByte())
            }
            writePacket(packet)
        }
    }
}