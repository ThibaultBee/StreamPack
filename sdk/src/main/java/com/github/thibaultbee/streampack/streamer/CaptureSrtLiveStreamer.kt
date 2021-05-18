/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.streamer

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.internal.endpoints.SrtProducer
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.utils.ILogger
import java.net.SocketException

/**
 * [BaseCaptureStreamer] that sends audio/video frames to a remote device p, Secure Reliable
 * Transport (SRT) Protocol.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 */
class CaptureSrtLiveStreamer(
    context: Context,
    tsServiceInfo: ServiceInfo,
    logger: ILogger
) : BaseCaptureStreamer(context, tsServiceInfo, SrtProducer(logger), logger) {
    /**
     * Listener to manage SRT connection.
     */
    var onConnectionListener: OnConnectionListener? = null
        set(value) {
            srtProducer.onConnectionListener = value
            field = value
        }

    private val srtProducer = endpoint as SrtProducer

    /**
     * Connect to an SRT server with correct Live streaming parameters.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    fun connect(ip: String, port: Int) {
        srtProducer.connect(ip, port)
    }

    /**
     * Disconnect from the connected SRT server.
     *
     * @throws SocketException is not connected
     */
    fun disconnect() {
        srtProducer.disconnect()
    }

    /**
     * Connect to an SRT server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startStream(ip: String, port: Int) {
        connect(ip, port)
        startStream()
    }
}