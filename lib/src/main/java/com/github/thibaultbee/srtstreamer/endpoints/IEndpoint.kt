package com.github.thibaultbee.srtstreamer.endpoints

import com.github.thibaultbee.srtstreamer.interfaces.Controllable
import java.nio.ByteBuffer

interface IEndpoint : Controllable {
    /**
     * Writes a buffer to endpoint.
     * @param buffer buffer to write
     */
    fun write(buffer: ByteBuffer)
}