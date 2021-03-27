package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.interfaces.Controllable

interface IEndpoint : Controllable {

    /**
     * Configure endpoint bitrate, mainly for network endpoint.
     * @param startBitrate bitrate at the beginning of the communication
     */
    fun configure(startBitrate: Int)

    /**
     * Writes a buffer to endpoint.
     * @param packet buffer to write
     */
    fun write(packet: Packet)
}