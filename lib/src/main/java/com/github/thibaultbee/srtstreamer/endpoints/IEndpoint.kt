package com.github.thibaultbee.srtstreamer.endpoints

import java.io.Closeable
import java.nio.ByteBuffer

interface IEndpoint : Runnable, Closeable {
    /**
     * Calls before a stream start (before first frame)
     * Throw an exception if the endpoint is not ready for live
     */
    override fun run()

    /**
     * Writes a buffer to endpoint.
     * @param buffer buffer to write
     */
    fun write(buffer: ByteBuffer)

    /**
     * Calls after a stream has been stop
     */
    fun stop()
}