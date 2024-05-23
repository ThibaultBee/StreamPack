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
package io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.composites.sinks

import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.ILiveSink
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import video.api.rtmpdroid.Rtmp
import java.util.concurrent.Executors

class RtmpSink(
    private val dispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher()
) : ILiveSink {
    override var onConnectionListener: OnConnectionListener? = null

    private var socket = Rtmp()
    private var isOnError = false

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened
    
    override val metrics: Any
        get() = TODO("Not yet implemented")

    override fun configure(config: EndpointConfiguration) {
        val videoConfig = config.streamConfigs.firstOrNull { it is VideoConfig }
        if (videoConfig != null) {
            socket.supportedVideoCodecs = listOf(videoConfig.mimeType)
        }
    }

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        require(!isOpened.value) { "SrtEndpoint is already opened" }
        require(mediaDescriptor.type.sinkType == MediaSinkType.RTMP) { "MediaDescriptor must be a rtmp Uri" }

        withContext(dispatcher) {
            try {
                isOnError = false
                socket.connect("${mediaDescriptor.uri} live=1 flashver=FMLE/3.0\\20(compatible;\\20FMSc/1.0)")
                _isOpened.emit(true)
            } catch (e: Exception) {
                socket = Rtmp()
                _isOpened.emit(false)
                throw e
            }
        }
    }

    override suspend fun write(packet: Packet) = withContext(dispatcher) {
        if (isOnError) {
            return@withContext
        }

        if (!(isOpened.value)) {
            Logger.w(TAG, "Socket is not connected, dropping packet")
            return@withContext
        }

        try {
            socket.write(packet.buffer)
        } catch (e: Exception) {
            close()
            isOnError = true
            _isOpened.emit(false)
            Logger.e(TAG, "Error while writing packet to socket", e)
            throw e
        }
    }

    override suspend fun startStream() {
        withContext(dispatcher) {
            socket.connectStream()
        }
    }

    override suspend fun stopStream() {
        // No need to stop stream
    }

    override suspend fun close() {
        withContext(dispatcher) {
            socket.close()
            _isOpened.emit(false)
            socket = Rtmp()
        }
    }

    companion object {
        private const val TAG = "RtmpSink"
    }
}
