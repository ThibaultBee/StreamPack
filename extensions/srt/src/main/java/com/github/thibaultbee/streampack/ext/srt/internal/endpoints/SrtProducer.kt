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
package com.github.thibaultbee.streampack.ext.srt.internal.endpoints

import com.github.thibaultbee.srtdroid.Srt
import com.github.thibaultbee.srtdroid.enums.Boundary
import com.github.thibaultbee.srtdroid.enums.ErrorType
import com.github.thibaultbee.srtdroid.enums.SockOpt
import com.github.thibaultbee.srtdroid.enums.Transtype
import com.github.thibaultbee.srtdroid.listeners.SocketListener
import com.github.thibaultbee.srtdroid.models.MsgCtrl
import com.github.thibaultbee.srtdroid.models.Socket
import com.github.thibaultbee.srtdroid.models.Stats
import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.data.SrtPacket
import com.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.logger.ILogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress

class SrtProducer(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val logger: ILogger
) : IEndpoint {
    var onConnectionListener: OnConnectionListener? = null

    private var socket = Socket()
    private var bitrate = 0L

    companion object {
        private const val PAYLOAD_SIZE = 1316
    }


    /**
     * Get/set SRT stream ID
     */
    var streamId: String
        get() = socket.getSockFlag(SockOpt.STREAMID) as String
        set(value) = socket.setSockFlag(SockOpt.STREAMID, value)


    /**
     * Get/set SRT stream passPhrase
     */
    var passPhrase: String
        get() = socket.getSockFlag(SockOpt.PASSPHRASE) as String
        set(value) = socket.setSockFlag(SockOpt.PASSPHRASE, value)


    /**
     * Get SRT stats
     */
    val stats: Stats
        get() = socket.bistats(clear = true, instantaneous = true)

    override fun configure(startBitrate: Int) {
        this.bitrate = startBitrate.toLong()
    }

    suspend fun connect(ip: String, port: Int) = withContext(coroutineDispatcher) {
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
            socket.connect(ip, port)
            onConnectionListener?.onSuccess()
        } catch (e: Exception) {
            socket = Socket()
            onConnectionListener?.onFailed(e.message ?: "Unknown error")
            throw e
        }

    }

    fun disconnect() {
        socket.close()
        socket = Socket()
    }

    override fun write(packet: Packet) {
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
        socket.send(packet.buffer, msgCtrl)
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