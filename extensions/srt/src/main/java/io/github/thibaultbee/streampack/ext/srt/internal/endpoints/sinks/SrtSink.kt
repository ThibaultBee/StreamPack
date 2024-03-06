package io.github.thibaultbee.streampack.ext.srt.internal.endpoints.sinks

import io.github.thibaultbee.srtdroid.Srt
import io.github.thibaultbee.srtdroid.enums.Boundary
import io.github.thibaultbee.srtdroid.enums.ErrorType
import io.github.thibaultbee.srtdroid.enums.SockOpt
import io.github.thibaultbee.srtdroid.enums.Transtype
import io.github.thibaultbee.srtdroid.listeners.SocketListener
import io.github.thibaultbee.srtdroid.models.MsgCtrl
import io.github.thibaultbee.srtdroid.models.Socket
import io.github.thibaultbee.srtdroid.models.Stats
import io.github.thibaultbee.streampack.ext.srt.data.SrtConnectionDescriptor
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.data.SrtPacket
import io.github.thibaultbee.streampack.internal.endpoints.sinks.ILiveSink
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress

class SrtSink(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ILiveSink {
    override var onConnectionListener: OnConnectionListener? = null

    private var socket = Socket()
    private var bitrate = 0L
    private var isOnError = false

    /**
     * Get/set SRT stream ID
     */
    var streamId: String
        get() = socket.getSockFlag(SockOpt.STREAMID) as String
        @Deprecated("Use SrtConnectionDescriptor.streamId instead")
        set(value) = socket.setSockFlag(SockOpt.STREAMID, value)

    /**
     * Get/set SRT stream passPhrase
     * It is a set only parameter, so getting the value throws an exception.
     */
    var passPhrase: String
        get() = socket.getSockFlag(SockOpt.PASSPHRASE) as String
        @Deprecated("Use SrtConnectionDescriptor.passPhrase instead")
        set(value) = socket.setSockFlag(SockOpt.PASSPHRASE, value)

    /**
     * Get/set bidirectional latency in milliseconds
     */
    var latency: Int
        get() = socket.getSockFlag(SockOpt.LATENCY) as Int
        private set(value) = socket.setSockFlag(SockOpt.LATENCY, value)

    /**
     * Get/set connection timeout in milliseconds
     */
    var connectionTimeout: Int
        get() = socket.getSockFlag(SockOpt.CONNTIMEO) as Int
        private set(value) = socket.setSockFlag(SockOpt.CONNTIMEO, value)

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

    override suspend fun connect(url: String) = connect(SrtConnectionDescriptor.fromUrl(url))

    suspend fun connect(connection: SrtConnectionDescriptor) {
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

                connection.streamId?.let { streamId = it }
                connection.passPhrase?.let { passPhrase = it }
                connection.latency?.let { latency = it }
                connection.connectionTimeout?.let { connectionTimeout = it }

                isOnError = false
                socket.connect(connection.host, connection.port)
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

    override suspend fun startStream() {
        if (!socket.isConnected) {
            throw ConnectException("SrtEndpoint should be connected at this point")
        }

        socket.setSockFlag(SockOpt.MAXBW, 0L)
        socket.setSockFlag(SockOpt.INPUTBW, bitrate)
    }

    override suspend fun stopStream() {

    }

    override fun release() {
        Srt.cleanUp()
    }

    companion object {
        private const val PAYLOAD_SIZE = 1316
    }
}