/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.ext.srt.internal.endpoints.composites.sinks

import io.github.thibaultbee.srtdroid.core.enums.Boundary
import io.github.thibaultbee.srtdroid.core.enums.SockOpt
import io.github.thibaultbee.srtdroid.core.enums.Transtype
import io.github.thibaultbee.srtdroid.core.models.MsgCtrl
import io.github.thibaultbee.srtdroid.core.models.SrtUrl.Mode
import io.github.thibaultbee.srtdroid.core.models.Stats
import io.github.thibaultbee.srtdroid.ktx.CoroutineSrtSocket
import io.github.thibaultbee.srtdroid.ktx.extensions.connect
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.error.ClosedException
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.data.SrtPacket
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ISinkInternal
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

class SrtSink : ISinkInternal {
    private var socket: CoroutineSrtSocket? = null
    private var completionException: Throwable? = null
    private var isOnError: Boolean = false

    private var bitrate = 0L

    /**
     * Get SRT stats
     */
    override val metrics: Stats
        get() = socket?.bistats(clear = true, instantaneous = true)
            ?: throw IllegalStateException("Socket is not initialized")

    private val _isOpen = MutableStateFlow(false)
    override val isOpen: StateFlow<Boolean> = _isOpen

    override fun configure(config: EndpointConfiguration) {
        bitrate = config.streamConfigs.sumOf { it.startBitrate.toLong() }
    }

    override suspend fun open(
        mediaDescriptor: MediaDescriptor
    ) = open(SrtMediaDescriptor(mediaDescriptor))

    private suspend fun open(mediaDescriptor: SrtMediaDescriptor) {
        if (isOpen.value) {
            Logger.w(TAG, "SrtSink is already opened")
            return
        }

        if (mediaDescriptor.srtUrl.mode != null) {
            require(mediaDescriptor.srtUrl.mode == Mode.CALLER) { "Invalid mode: ${mediaDescriptor.srtUrl.mode}. Only caller supported." }
        }
        if (mediaDescriptor.srtUrl.payloadSize != null) {
            require(mediaDescriptor.srtUrl.payloadSize == PAYLOAD_SIZE)
        }
        if (mediaDescriptor.srtUrl.transtype != null) {
            require(mediaDescriptor.srtUrl.transtype == Transtype.LIVE)
        }

        socket = CoroutineSrtSocket().apply {
            // Forces this value. Only works if they are null in [srtUrl]
            setSockFlag(SockOpt.PAYLOADSIZE, PAYLOAD_SIZE)
            setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)
            completionException = null
            isOnError = false
            socketContext.invokeOnCompletion { t ->
                completionException = t
                runBlocking {
                    this@SrtSink.close()
                }
            }
            connect(mediaDescriptor.srtUrl)
        }
        _isOpen.emit(true)
    }

    override suspend fun write(packet: Packet): Int {
        if (isOnError) {
            return -1
        }

        completionException?.let {
            isOnError = true
            throw ClosedException(it)
        }

        val socket = requireNotNull(socket) { "SrtEndpoint is not initialized" }

        val boundary = if (packet is SrtPacket) {
            when {
                packet.isFirstPacketFrame && packet.isLastPacketFrame -> Boundary.SOLO
                packet.isFirstPacketFrame -> Boundary.FIRST
                packet.isLastPacketFrame -> Boundary.LAST
                else -> Boundary.SUBSEQUENT
            }
        } else {
            null
        }
        val msgCtrl =
            if (packet.ts == 0L) {
                if (boundary != null) {
                    MsgCtrl(boundary = boundary)
                } else {
                    MsgCtrl()
                }
            } else {
                if (boundary != null) {
                    MsgCtrl(
                        ttl = 500,
                        srcTime = packet.ts,
                        boundary = boundary
                    )
                } else {
                    MsgCtrl(
                        ttl = 500,
                        srcTime = packet.ts
                    )
                }
            }

        try {
            return socket.send(packet.buffer, msgCtrl)
        } catch (t: Throwable) {
            isOnError = true
            close()
            throw ClosedException(t)
        }
    }

    override suspend fun startStream() {
        val socket = requireNotNull(socket) { "SrtEndpoint is not initialized" }
        require(socket.isConnected) { "SrtEndpoint should be connected at this point" }

        socket.setSockFlag(SockOpt.MAXBW, 0L)
        socket.setSockFlag(SockOpt.INPUTBW, bitrate)
    }

    override suspend fun stopStream() {
    }

    override suspend fun close() {
        socket?.close()
        _isOpen.emit(false)
    }


    companion object {
        private const val TAG = "SrtSink"

        private const val PAYLOAD_SIZE = 1316
    }
}