package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.srtdroid.Srt
import com.github.thibaultbee.srtdroid.enums.SockOpt
import com.github.thibaultbee.srtdroid.enums.Transtype
import com.github.thibaultbee.srtdroid.models.Socket
import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.utils.Logger
import java.net.ConnectException
import java.nio.ByteBuffer

class SrtProducer(val logger: Logger) : IEndpoint {
    private var socket = Socket()
    private var sendBuffer = ByteBuffer.allocateDirect(PAYLOAD_SIZE)
    private var bitrate = 0

    companion object {
        private const val PAYLOAD_SIZE = 1316
    }

    override fun configure(startBitrate: Int) {
        this.bitrate = startBitrate
    }

    fun connect(ip: String, port: Int) {
        socket = Socket()
        socket.setSockFlag(SockOpt.PAYLOADSIZE, PAYLOAD_SIZE)
        socket.setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)
        socket.connect(ip, port)
    }

    override fun write(packet: Packet) {
        sendBuffer.put(packet.buffer)
        if (!sendBuffer.hasRemaining() || packet.isLastPacketFrame) {
            sendBuffer()
        }
    }

    private fun sendBuffer() {
        sendBuffer.limit(sendBuffer.position())
        sendBuffer.rewind()
        socket.send(sendBuffer)
        sendBuffer.limit(PAYLOAD_SIZE)
    }

    private fun flush() {
        // There is something in the buffer: flush it.
        if (sendBuffer.position() != 0) {
            sendBuffer()
        }
    }

    fun disconnect() {
        socket.close()
    }

    override fun startStream() {
        // socket.setSockFlag(SockOpt.INPUTBW, bitrate)
        if (!socket.isConnected)
            throw ConnectException("SrtEndpoint should be connected at this point")
    }

    override fun stopStream() {
        flush()
    }

    override fun release() {
        Srt.cleanUp()
    }
}