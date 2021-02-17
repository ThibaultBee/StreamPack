package com.github.thibaultbee.streampack.interfaces

import java.io.Closeable

interface Controllable : Runnable, Closeable {
    /**
     * Runs frames or data stream generation
     * Throw an exception if not ready for live stream
     */
    override fun run()

    /**
     * Stops frames or data stream generation
     */
    fun stop()

    /**
     * Closes and releases resources
     */
    override fun close()
}