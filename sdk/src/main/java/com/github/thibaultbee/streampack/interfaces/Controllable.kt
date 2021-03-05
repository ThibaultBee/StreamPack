package com.github.thibaultbee.streampack.interfaces

interface Controllable {
    /**
     * Starts frames or data stream generation
     * Throw an exception if not ready for live stream
     */
    fun startStream()

    /**
     * Stops frames or data stream generation
     */
    fun stopStream()

    /**
     * Closes and releases resources
     */
    fun release()
}