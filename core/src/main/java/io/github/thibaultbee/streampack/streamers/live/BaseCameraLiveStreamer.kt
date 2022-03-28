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

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.internal.endpoints.FileWriter
import io.github.thibaultbee.streampack.internal.endpoints.ILiveEndpoint
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer

/**
 * A [BaseCameraStreamer] that sends microphone and camera frames to a remote device.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param muxer a [IMuxer] implementation
 * @param endpoint a [ILiveEndpoint] implementation
 */
open class BaseCameraLiveStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
    muxer: IMuxer,
    endpoint: ILiveEndpoint
) : BaseCameraStreamer(
    context = context,
    logger = logger,
    enableAudio = enableAudio,
    muxer = muxer,
    endpoint = endpoint
),
    ILiveStreamer {
    private val liveProducer = endpoint

    /**
     * Listener to manage connection.
     */
    override var onConnectionListener: OnConnectionListener? = null
        set(value) {
            liveProducer.onConnectionListener = value
            field = value
        }

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
     * @param url server url (syntax: rtmp://server/streamKey or srt://ip:port)
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    override suspend fun startStream(url: String) {
        connect(url)
        startStream()
    }

    abstract class Builder : BaseCameraStreamer.Builder() {
        protected lateinit var endpoint: ILiveEndpoint

        /**
         * Set live endpoint.
         * Mandatory.
         *
         * @param endpoint a [ILiveEndpoint] implementation
         */
        protected fun setLiveEndpointImpl(endpoint: ILiveEndpoint) =
            apply { this.endpoint = endpoint }

        /**
         * Combines all of the characteristics that have been set and return a new
         * generic [BaseCameraLiveStreamer] object.
         *
         * @return a new generic [BaseCameraLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): BaseCameraLiveStreamer {
            return BaseCameraLiveStreamer(
                context,
                logger,
                enableAudio,
                muxer,
                endpoint
            ).also { streamer ->
                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }

                previewSurface?.let {
                    streamer.startPreview(it)
                }
            }
        }
    }
}