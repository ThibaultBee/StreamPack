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

import io.github.thibaultbee.srtdroid.enums.Boundary
import io.github.thibaultbee.srtdroid.enums.ErrorType
import io.github.thibaultbee.srtdroid.enums.SockOpt
import io.github.thibaultbee.srtdroid.enums.Transtype
import io.github.thibaultbee.srtdroid.listeners.SocketListener
import io.github.thibaultbee.srtdroid.models.MsgCtrl
import io.github.thibaultbee.srtdroid.models.Socket
import io.github.thibaultbee.srtdroid.models.Stats
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.data.SrtPacket
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ISink
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress

class SrtSink(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ISink {
    private var socket = Socket()
    private var bitrate = 0L
    private var isOnError = false

    /**
     * Get/set SRT stream ID
     */
    private var streamId: String
        get() = socket.getSockFlag(SockOpt.STREAMID) as String
        private set(value) = socket.setSockFlag(SockOpt.STREAMID, value)

    /**
     * Get/set SRT stream passPhrase
     * It is a set only parameter, so getting the value throws an exception.
     */
    private var passPhrase: String
        get() = socket.getSockFlag(SockOpt.PASSPHRASE) as String
        private set(value) = socket.setSockFlag(SockOpt.PASSPHRASE, value)

    /**
     * Get/set bidirectional latency in milliseconds
     */
    private var latency: Int
        get() = socket.getSockFlag(SockOpt.LATENCY) as Int
        private set(value) = socket.setSockFlag(SockOpt.LATENCY, value)

    /**
     * Get/set connection timeout in milliseconds
     */
    private var connectionTimeout: Int
        get() = socket.getSockFlag(SockOpt.CONNTIMEO) as Int
        private set(value) = socket.setSockFlag(SockOpt.CONNTIMEO, value)

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

        withContext(coroutineDispatcher) {
            try {
                socket.listener = object : SocketListener {
                    override fun onConnectionLost(
                        ns: Socket,
                        error: ErrorType,
                        peerAddress: InetSocketAddress,
                        token: Int
                    ) {
                        socket = Socket()
                        runBlocking {
                            _isOpened.emit(false)
                        }
                    }

                    override fun onListen(
                        ns: Socket,
                        hsVersion: Int,
                        peerAddress: InetSocketAddress,
                        streamId: String
                    ) = 0 // Only for server - not needed here
                }
                socket.setSockFlag(SockOpt.PAYLOADSIZE, PAYLOAD_SIZE)
                socket.setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)

                mediaDescriptor.streamId?.let { streamId = it }
                mediaDescriptor.passPhrase?.let { passPhrase = it }
                mediaDescriptor.latency?.let { latency = it }
                mediaDescriptor.connectionTimeout?.let { connectionTimeout = it }

                isOnError = false
                socket.connect(mediaDescriptor.host, mediaDescriptor.port)
                _isOpened.emit(true)
            } catch (e: Exception) {
                socket = Socket()
                throw e
            }
        }
    }

    override suspend fun write(packet: Packet) {
        if (isOnError) return

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
            socket.send(packet.buffer, msgCtrl)
        } catch (e: Exception) {
            _isOpened.emit(false)
            isOnError = true
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
        socket = Socket()
    }


    companion object {
        private const val PAYLOAD_SIZE = 1316
    }
}