package com.github.thibaultbee.streampack.streamers.interfaces

import com.github.thibaultbee.streampack.listeners.OnConnectionListener

interface ILiveStreamer {
    /**
     * Listen to manage connection events.
     */
    var onConnectionListener: OnConnectionListener?

    /**
     * Get/set connection passphrase.
     */
    var passPhrase: String

    /**
     * Get/set stream id.
     */
    var streamId: String

    /**
     * Connect to a remote server.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    suspend fun connect(ip: String, port: Int)

    /**
     * Disconnect from the remote server.
     *
     * @throws Exception is not connected
     */
    fun disconnect()

    /**
     * Same as [connect] then [startStream].
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed or startStream failed.
     */
    suspend fun startStream(ip: String, port: Int)
}