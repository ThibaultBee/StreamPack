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

import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.error.ClosedException
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ISinkInternal
import io.github.thibaultbee.streampack.core.logger.Logger
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
) : ISinkInternal {
    private var socket: Rtmp? = null
    private var isOnError = false

    private val supportedVideoCodecs = mutableListOf<String>()

    private val _isOpen = MutableStateFlow(false)
    override val isOpen: StateFlow<Boolean> = _isOpen

    override val metrics: Any
        get() = TODO("Not yet implemented")

    override fun configure(config: EndpointConfiguration) {
        val videoConfig = config.streamConfigs.firstOrNull { it is VideoConfig }
        if (videoConfig != null) {
            supportedVideoCodecs.clear()
            supportedVideoCodecs += listOf(videoConfig.mimeType)
        }
    }

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        if (isOpen.value) {
            Logger.w(TAG, "RtmpSink is already opened")
            return
        }

        require(mediaDescriptor.type.sinkType == MediaSinkType.RTMP) { "MediaDescriptor must be a rtmp Uri" }

        withContext(dispatcher) {
            isOnError = false
            socket = Rtmp().apply {
                /**
                 * TODO: Add supportedVideoCodecs to Rtmp
                 * The workaround could be to split connect and connect message.
                 * The first would be call here and the connect message would be send in
                 * [startStream] (when configure has been called).
                 */
                // supportedVideoCodecs = this@RtmpSink.supportedVideoCodecs
                connect("${mediaDescriptor.uri} live=1 flashver=FMLE/3.0\\20(compatible;\\20FMSc/1.0)")
            }
            _isOpen.emit(true)
        }
    }

    override suspend fun write(packet: Packet) =
        withContext(dispatcher) {
            if (isOnError) {
                return@withContext -1
            }

            if (!(isOpen.value)) {
                Logger.w(TAG, "Socket is not connected, dropping packet")
                return@withContext -1
            }

            val socket = requireNotNull(socket) { "Socket is not initialized" }

            try {
                return@withContext socket.write(packet.buffer)
            } catch (t: Throwable) {
                isOnError = true
                close()
                Logger.e(TAG, "Error while writing packet to socket", t)
                throw ClosedException(t)
            }
        }

    override suspend fun startStream() {
        withContext(dispatcher) {
            val socket = requireNotNull(socket) { "Socket is not initialized" }
            socket.connectStream()
        }
    }

    override suspend fun stopStream() {
        // No need to stop stream
    }

    override suspend fun close() {
        withContext(dispatcher) {
            socket?.close()
            _isOpen.emit(false)
        }
    }

    companion object {
        private const val TAG = "RtmpSink"
    }
}
