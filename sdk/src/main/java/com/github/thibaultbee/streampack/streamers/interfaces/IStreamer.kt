package com.github.thibaultbee.streampack.streamers.interfaces

import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.listeners.OnErrorListener

interface IStreamer {
    /**
     * Listener that reports streamer error.
     * Supports only one listener.
     */
    var onErrorListener: OnErrorListener?

    /**
     * Configures both video and audio settings.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [StreamPackError] if configuration can not be applied.
     * @see [release]
     */
    fun configure(audioConfig: AudioConfig? = null, videoConfig: VideoConfig? = null)

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

    /**
     * Clean and reset the streamer.
     *
     * @see [configure]
     */
    fun release()
}