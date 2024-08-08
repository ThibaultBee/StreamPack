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
import io.github.thibaultbee.srtdroid.core.enums.ErrorType
import io.github.thibaultbee.srtdroid.core.enums.SockOpt
import io.github.thibaultbee.srtdroid.core.enums.Transtype
import io.github.thibaultbee.srtdroid.core.models.MsgCtrl
import io.github.thibaultbee.srtdroid.core.models.SrtError
import io.github.thibaultbee.srtdroid.core.models.SrtUrl.Mode
import io.github.thibaultbee.srtdroid.core.models.Stats
import io.github.thibaultbee.srtdroid.ktx.CoroutineSrtSocket
import io.github.thibaultbee.srtdroid.ktx.extensions.connect
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.data.SrtPacket
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ISink
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

class SrtSink : ISink {
    private var socket = CoroutineSrtSocket()
    private var bitrate = 0L
    private val isOnError: Boolean
        get() = SrtError.lastError != ErrorType.SUCCESS

    /**
     * Get SRT stats
     */
    override val metrics: Stats
        get() = socket.bistats(clear = true, instantaneous = true)

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened

    override fun configure(config: EndpointConfiguration) {
        bitrate = config.streamConfigs.sumOf { it.startBitrate.toLong() }
    }

    override suspend fun open(
        mediaDescriptor: MediaDescriptor
    ) = open(SrtMediaDescriptor(mediaDescriptor))

    private suspend fun open(mediaDescriptor: SrtMediaDescriptor) {
        require(!isOpened.value) { "Sink is already opened" }
        if (mediaDescriptor.srtUrl.mode != null) {
            require(mediaDescriptor.srtUrl.mode == Mode.CALLER) { "Invalid mode: ${mediaDescriptor.srtUrl.mode}. Only caller supported." }
        }
        if (mediaDescriptor.srtUrl.payloadSize != null) {
            require(mediaDescriptor.srtUrl.payloadSize == PAYLOAD_SIZE)
        }
        if (mediaDescriptor.srtUrl.transtype != null) {
            require(mediaDescriptor.srtUrl.transtype == Transtype.LIVE)
        }

        try {
            socket = CoroutineSrtSocket().apply {
                // Forces this value. Only works if they are null in [srtUrl]
                setSockFlag(SockOpt.PAYLOADSIZE, PAYLOAD_SIZE)
                setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)
            }
            socket.socketContext.invokeOnCompletion {
                runBlocking {
                    _isOpened.emit(false)
                }
            }
            socket.connect(mediaDescriptor.srtUrl)
            _isOpened.emit(true)
        } catch (e: Exception) {
            socket = CoroutineSrtSocket()
            throw e
        }
    }

    override suspend fun write(packet: Packet): Int {
        if (isOnError) return -1

        packet as SrtPacket
        val boundary = when {
            packet.isFirstPacketFrame && packet.isLastPacketFrame -> Boundary.SOLO
            packet.isFirstPacketFrame -> Boundary.FIRST
            packet.isLastPacketFrame -> Boundary.LAST
            else -> Boundary.SUBSEQUENT
        }
        val msgCtrl =
            if (packet.ts == 0L) {
                MsgCtrl(boundary = boundary)
            } else {
                MsgCtrl(
                    ttl = 500,
                    srcTime = packet.ts,
                    boundary = boundary
                )
            }

        try {
            return socket.send(packet.buffer, msgCtrl)
        } catch (e: Exception) {
            _isOpened.emit(false)
            throw e
        }
    }

    override suspend fun startStream() {
        require(socket.isConnected) { "SrtEndpoint should be connected at this point" }

        socket.setSockFlag(SockOpt.MAXBW, 0L)
        socket.setSockFlag(SockOpt.INPUTBW, bitrate)
    }

    override suspend fun stopStream() {
    }

    override suspend fun close() {
        socket.close()
        _isOpened.emit(false)
    }


    companion object {
        private const val PAYLOAD_SIZE = 1316
    }
}