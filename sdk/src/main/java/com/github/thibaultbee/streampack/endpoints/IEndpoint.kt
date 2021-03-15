package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.interfaces.Controllable
import java.nio.ByteBuffer

interface IEndpoint : Controllable {

    /**
     * Configure endpoint bitrate, mainly for network endpoint.
     * @param startBitrate bitrate at the beginning of the communication
     */
    fun configure(startBitrate: Int)

    /**
     * Writes a buffer to endpoint.
     * @param buffer buffer to write
     */
    fun write(buffer: ByteBuffer)
}