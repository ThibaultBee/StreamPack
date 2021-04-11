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
package com.github.thibaultbee.streampack

import android.content.Context
import com.github.thibaultbee.streampack.endpoints.SrtProducer
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.Logger
import java.net.SocketException

class CaptureSrtLiveStream(
    context: Context,
    tsServiceInfo: ServiceInfo,
    logger: Logger
) : BaseCaptureStream(context, tsServiceInfo, SrtProducer(logger), logger) {
    private val srtProducer = endpoint as SrtProducer

    /**
     * Connect to an SRT server with correct Live streaming parameters
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    fun connect(ip: String, port: Int) {
        srtProducer.connect(ip, port)
    }

    /**
     * Disconnect from the connected SRT server
     * @throws SocketException is not connected
     */
    fun disconnect() {
        srtProducer.disconnect()
    }

    /**
     * Connect to an SRT server and start stream.
     * Same as calling [connect] and [startStream].
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    fun startStream(ip: String, port: Int) {
        connect(ip, port)
        startStream()
    }
}