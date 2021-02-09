package com.github.thibaultbee.srtstreamer.mux.ts.tables

import com.github.thibaultbee.srtstreamer.interfaces.MuxListener
import com.github.thibaultbee.srtstreamer.mux.ts.data.ITSElement
import com.github.thibaultbee.srtstreamer.mux.ts.data.Service
import net.magik6k.bitbuffer.BitBuffer
import java.nio.ByteBuffer

class Pat(
    muxListener: MuxListener,
    private val services: List<Service>,
    tsId: Short,
    versionNumber: Byte = 0,
    var packetCount: Int = 0,
) : Psi(
    muxListener,
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
            write(asByteBuffer())
        }
    }

    override fun asByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())

        services
            .filter { it.pmt != null }
            .forEach {
                buffer.put(it.info.id)
                buffer.put(0b111, 3)  // reserved
                buffer.put(it.pmt!!.pid, 13)
            }

        return buffer.asByteBuffer()
    }
}