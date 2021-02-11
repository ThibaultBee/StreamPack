package com.github.thibaultbee.srtstreamer.mux.ts.packets

import com.github.thibaultbee.srtstreamer.mux.IMuxListener
import com.github.thibaultbee.srtstreamer.mux.ts.descriptors.AdaptationField
import com.github.thibaultbee.srtstreamer.mux.ts.utils.TSOutputCallback
import com.github.thibaultbee.srtstreamer.utils.hasRemaining
import com.github.thibaultbee.srtstreamer.utils.put
import com.github.thibaultbee.srtstreamer.utils.remaining
import net.magik6k.bitbuffer.BitBuffer
import java.nio.ByteBuffer

open class TS(
    muxListener: IMuxListener,
    val pid: Short,
    private val transportErrorIndicator: Boolean = false,
    private val transportPriority: Boolean = false,
    private val transportScramblingControl: Byte = 0, // Not scrambled
    private var continuityCounter: Byte = 0,
) : TSOutputCallback(muxListener) {

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
            val packet = BitBuffer.allocate(Byte.SIZE_BITS * PACKET_SIZE.toLong())

            // Write header to packet
            packet.put(SYNC_BYTE, 8)
            packet.put(transportErrorIndicator)
            packet.put(payloadUnitStartIndicator)
            payloadUnitStartIndicator = false
            packet.put(transportPriority)
            packet.put(pid, 13)
            packet.put(transportScramblingControl, 2)

            when {
                adaptationFieldIndicator and payloadIndicator -> {
                    packet.put(0b11, 2)
                }
                adaptationFieldIndicator -> {
                    packet.put(0b10, 2)
                }
                payloadIndicator -> {
                    packet.put(0b01, 2)
                }
            }

            packet.put(continuityCounter, 4)
            continuityCounter = ((continuityCounter + 1) and 0xF).toByte()

            // Add adaptation fields first if needed
            if (adaptationFieldIndicator) {
                packet.put(adaptationField!!) // Is not null if adaptationFieldIndicator is true
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
                        val headerLength = packet.position() / Byte.SIZE_BITS
                        packet.setPosition(26)
                        packet.putBoolean(true) // adaptation_field_control
                        packet.setPosition(32)
                        val stuffingLength = PACKET_SIZE - it.remaining() - headerLength - 1
                        packet.put(stuffingLength, 8)
                        if (stuffingLength >= 2) {
                            packet.put(0.toByte())
                            while (packet.position() < ((PACKET_SIZE - it.remaining()) * Byte.SIZE_BITS).toLong()) {
                                packet.put(0xFF.toByte()) // Stuffing
                            }
                        }
                    }
                }

                it.limit(it.position() + packet.remaining().toInt().coerceAtMost(it.remaining()))
                packet.put(it)
                it.limit(payloadLimit)
            }

            while (packet.hasRemaining()) {
                packet.put(0xFF.toByte())
            }
            writePacket(packet.asByteBuffer())
        }
    }
}