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
package io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.api.rtmpdroid.Rtmp

class RtmpProducer(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val logger: ILogger
) :
    IEndpoint {
    var onConnectionListener: OnConnectionListener? = null

    private var socket = Rtmp()

    override fun configure(startBitrate: Int) {
    }

    suspend fun connect(url: String) = withContext(coroutineDispatcher) {
        try {
            socket.connect("$url live=1")
            onConnectionListener?.onSuccess()
        } catch (e: Exception) {
            socket = Rtmp()
            onConnectionListener?.onFailed(e.message ?: "Unknown error")
            throw e
        }
    }

    fun disconnect() {
        socket.close()
        socket = Rtmp()
    }

    override fun write(packet: Packet) {
        socket.write(packet.buffer)
    }

    override fun startStream() {
        socket.connectStream()
    }

    override fun stopStream() {
        socket.deleteStream()
    }

    override fun release() {
    }
}