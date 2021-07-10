package com.github.thibaultbee.streampack.streamers.interfaces

interface IStreamer {
    /**
     * Starts audio/video stream.
     *
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @see [stopStream]
     */
    fun startStream()

    /**
     * Stops audio/video stream.
     *
     * @see [startStream]
     */
    fun stopStream()
}