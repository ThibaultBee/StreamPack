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
import io.github.thibaultbee.streampack.internal.endpoints.ILiveEndpoint
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer

/**
 * A [BaseScreenRecorderStreamer] that sends microphone and screen frames to a remote device.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param muxer a [IMuxer] implementation
 * @param endpoint a [ILiveEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 * @param initialOnConnectionListener initialize [OnConnectionListener]
 */
open class BaseScreenRecorderLiveStreamer(
    context: Context,
    enableAudio: Boolean = true,
    muxer: IMuxer,
    endpoint: ILiveEndpoint,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : BaseScreenRecorderStreamer(
    context = context,
    enableAudio = enableAudio,
    muxer = muxer,
    endpoint = endpoint,
    initialOnErrorListener = initialOnErrorListener
),
    ILiveStreamer {
    private val liveProducer = endpoint.apply { onConnectionListener = initialOnConnectionListener }

    /**
     * Listener to manage connection.
     */
    override var onConnectionListener: OnConnectionListener? = initialOnConnectionListener
        set(value) {
            liveProducer.onConnectionListener = value
            field = value
        }

    /**
     * Check if the streamer is connected to the server.
     */
    override val isConnected: Boolean
        get() = liveProducer.isConnected

    /**
     * Connect to an remove server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(url: String) {
        liveProducer.connect(url)
    }

    /**
     * Disconnect from the remote server.
     *
     * @throws Exception is not connected
     */
    override fun disconnect() {
        liveProducer.disconnect()
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