package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.interfaces.Controllable
import java.nio.ByteBuffer

interface IEndpoint : Controllable {
    /**
     * Writes a buffer to endpoint.
     * @param buffer buffer to write
     */
    fun write(buffer: ByteBuffer)
}