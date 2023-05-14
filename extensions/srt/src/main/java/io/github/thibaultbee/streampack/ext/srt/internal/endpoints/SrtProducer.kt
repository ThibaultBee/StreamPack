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
package io.github.thibaultbee.streampack.ext.srt.internal.endpoints

import android.net.Uri
import io.github.thibaultbee.srtdroid.Srt
import io.github.thibaultbee.srtdroid.enums.Boundary
import io.github.thibaultbee.srtdroid.enums.ErrorType
import io.github.thibaultbee.srtdroid.enums.SockOpt
import io.github.thibaultbee.srtdroid.enums.Transtype
import io.github.thibaultbee.srtdroid.listeners.SocketListener
import io.github.thibaultbee.srtdroid.models.MsgCtrl
import io.github.thibaultbee.srtdroid.models.Socket
import io.github.thibaultbee.srtdroid.models.Stats
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.data.SrtPacket
import io.github.thibaultbee.streampack.internal.endpoints.ILiveEndpoint
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress
import java.security.InvalidParameterException

class SrtProducer(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ILiveEndpoint {
    override var onConnectionListener: OnConnectionListener? = null

    private var socket = Socket()
    private var bitrate = 0L
    private var isOnError = false

    companion object {
        private const val PAYLOAD_SIZE = 1316

        private const val SRT_SCHEME = "srt"
        private const val SRT_PREFIX = "$SRT_SCHEME://"
    }

    /**
     * Get/set SRT stream ID
     */
    var streamId: String
        get() = socket.getSockFlag(SockOpt.STREAMID) as String
        set(value) = socket.setSockFlag(SockOpt.STREAMID, value)

    /**
     * Get/set SRT stream passPhrase
     * It is a set only parameter, so getting the value throws an exception.
     */
    var passPhrase: String
        get() = socket.getSockFlag(SockOpt.PASSPHRASE) as String
        set(value) = socket.setSockFlag(SockOpt.PASSPHRASE, value)

    /**
     * Get/set bidirectional latency in milliseconds
     */
    var latency: Int
        get() = socket.getSockFlag(SockOpt.LATENCY) as Int
        set(value) = socket.setSockFlag(SockOpt.LATENCY, value)

    /**
     * Get SRT stats
     */
    val stats: Stats
        get() = socket.bistats(clear = true, instantaneous = true)

    override val isConnected: Boolean
        get() = socket.isConnected

    override fun configure(config: Int) {
        this.bitrate = config.toLong()
    }

    override suspend fun connect(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != SRT_SCHEME) {
            throw InvalidParameterException("URL $url is not an srt URL")
        }
        uri.getQueryParameter("streamid")?.let { streamId = it }
        uri.getQueryParameter("passphrase")?.let { passPhrase = it }
        uri.getQueryParameter("latency")?.let { latency = it.toInt() }
        uri.host?.let { connect(it, uri.port) }
            ?: throw InvalidParameterException("Failed to parse URL $url: unknown host")
    }

    suspend fun connect(ip: String, port: Int) {
        withContext(coroutineDispatcher) {
            if (ip.isBlank()) {
                throw InvalidParameterException("Invalid IP $ip")
            }
            try {
                socket.listener = object : SocketListener {
                    override fun onConnectionLost(
                        ns: Socket,
                        error: ErrorType,
                        peerAddress: InetSocketAddress,
                        token: Int
                    ) {
                        socket = Socket()
                        onConnectionListener?.onLost(error.toString())
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
                isOnError = false
                socket.connect(ip.removePrefix(SRT_PREFIX), port)
                onConnectionListener?.onSuccess()
            } catch (e: Exception) {
                socket = Socket()
                onConnectionListener?.onFailed(e.message ?: "Unknown error")
                throw e
            }
        }
    }

    override fun disconnect() {
        socket.close()
        socket = Socket()
    }

    override fun write(packet: Packet) {
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
            isOnError = true
            throw e
        }
    }

    override fun startStream() {
        if (!socket.isConnected) {
            throw ConnectException("SrtEndpoint should be connected at this point")
        }

        socket.setSockFlag(SockOpt.MAXBW, 0L)
        socket.setSockFlag(SockOpt.INPUTBW, bitrate)
    }

    override fun stopStream() {

    }

    override fun release() {
        Srt.cleanUp()
    }
}