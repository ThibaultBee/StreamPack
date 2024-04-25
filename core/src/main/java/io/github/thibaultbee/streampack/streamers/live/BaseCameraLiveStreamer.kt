/*
 * Copyright (C) 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.streamers.live

import android.content.Context
import io.github.thibaultbee.streampack.internal.endpoints.IConnectableEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer

/**
 * A [BaseCameraStreamer] that sends microphone and camera frames to a remote device.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param internalEndpoint the [IConnectableEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 * @param initialOnConnectionListener initialize [OnConnectionListener]
 */
open class BaseCameraLiveStreamer(
    context: Context,
    enableAudio: Boolean = true,
    internalEndpoint: IConnectableEndpoint,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : BaseCameraStreamer(
    context = context,
    enableAudio = enableAudio,
    internalEndpoint = internalEndpoint,
    initialOnErrorListener = initialOnErrorListener
),
    ILiveStreamer {
    private val liveEndpoint = internalEndpoint.apply { onConnectionListener = initialOnConnectionListener }

    /**
     * Listener to manage connection.
     */
    override var onConnectionListener: OnConnectionListener? = initialOnConnectionListener
        set(value) {
            liveEndpoint.onConnectionListener = value
            field = value
        }

    /**
     * Check if the streamer is connected to the server.
     */
    override val isConnected: Boolean
        get() = liveEndpoint.isConnected

    /**
     * Connect to an remove server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(url: String) {
        liveEndpoint.connect(url)
    }

    /**
     * Disconnect from the remote server.
     *
     * @throws Exception is not connected
     */
    override fun disconnect() {
        liveEndpoint.disconnect()
    }

    /**
     * Connect to a remote server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url (syntax: rtmp://server/app/streamKey or srt://ip:port)
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    override suspend fun startStream(url: String) {
        connect(url)
        try {
            startStream()
        } catch (e: Exception) {
            disconnect()
            throw e
        }
    }
}