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
import io.github.thibaultbee.srtdroid.ktx.connect
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ClosedException
import io.github.thibaultbee.streampack.core.elements.data.Packet
import io.github.thibaultbee.streampack.core.elements.data.SrtPacket
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.AbstractSink
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.SinkConfiguration
import io.github.thibaultbee.streampack.ext.srt.data.mediadescriptor.SrtMediaDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class SrtSink : AbstractSink() {
    override val supportedSinkTypes: List<MediaSinkType> = listOf(MediaSinkType.SRT)

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

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    override fun configure(config: SinkConfiguration) {
        bitrate = config.streamConfigs.sumOf { it.startBitrate.toLong() }
    }

    override suspend fun openImpl(mediaDescriptor: MediaDescriptor) =
        open(SrtMediaDescriptor(mediaDescriptor))

    private suspend fun open(mediaDescriptor: SrtMediaDescriptor) {
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
        _isOpenFlow.emit(true)
    }

    private fun buildMsgCtrl(packet: Packet): MsgCtrl {
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
        return if (packet.ts == 0L) {
            if (boundary != null) {
                MsgCtrl(boundary = boundary)
            } else {
                MsgCtrl()
            }
        } else {
            if (boundary != null) {
                MsgCtrl(srcTime = packet.ts, boundary = boundary)
            } else {
                MsgCtrl(srcTime = packet.ts)
            }
        }
    }

    override suspend fun write(packet: Packet): Int {
        if (isOnError) {
            return -1
        }

        // Pick up completionException if any
        completionException?.let {
            isOnError = true
            throw ClosedException(it)
        }

        val socket = requireNotNull(socket) { "SrtEndpoint is not initialized" }

        try {
            return socket.send(packet.buffer, buildMsgCtrl(packet))
        } catch (t: Throwable) {
            isOnError = true
            if (completionException != null) {
                // Socket already closed
                throw ClosedException(completionException!!)
            }
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
        _isOpenFlow.emit(false)
    }


    companion object {
        private const val TAG = "SrtSink"

        private const val PAYLOAD_SIZE = 1316
    }
}