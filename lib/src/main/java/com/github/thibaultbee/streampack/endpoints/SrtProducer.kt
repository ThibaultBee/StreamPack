package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.srtdroid.Srt
import com.github.thibaultbee.srtdroid.enums.SockOpt
import com.github.thibaultbee.srtdroid.enums.Transtype
import com.github.thibaultbee.srtdroid.models.Socket
import com.github.thibaultbee.streampack.utils.Error
import com.github.thibaultbee.streampack.utils.Logger
import java.io.IOException
import java.net.ConnectException
import java.nio.ByteBuffer

class SrtProducer(val logger: Logger) : IEndpoint {
    private val srt = Srt()
    private var socket = Socket()
    private val sendBuffer = ByteBuffer.allocate(PAYLOAD_SIZE)

    companion object {
        private const val PAYLOAD_SIZE = 1316
    }

    init {
        srt.startUp()
    }

    fun connect(ip: String, port: Int): Error {
        socket = Socket()
        try {
            socket.setSockFlag(SockOpt.PAYLOADSIZE, PAYLOAD_SIZE)
            socket.setSockFlag(SockOpt.SNDSYN, true)
            socket.setSockFlag(SockOpt.RCVSYN, true)
            socket.setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)
            // socket.setSockFlag(SockOpt.MAXBW, 0)
            // socket.setSockFlag(SockOpt.TSBPDMODE, false)
            // socket.setSockFlag(SockOpt.INPUTBW, 1000000)
            // socket.setSockFlag(SockOpt.OHEADBW, 25)
        } catch (e: IOException) {
            e.printStackTrace()
            socket.close()
            logger.e(this, "Failed to configure for connection with $ip:$port: ${e.message}")
            return Error.CONFIGURATION_ERROR
        }

        try {
            socket.connect(ip, port)
        } catch (e: IOException) {
            socket.close()
            logger.e(this, "Failed to connect to $ip:$port: ${e.message}")
            return Error.CONNECTION_ERROR
        }

        return Error.SUCCESS
    }

    override fun write(buffer: ByteBuffer) {
        // Read Byte Buffer
        while (buffer.hasRemaining()) {
            sendBuffer.put(buffer)
            if (!sendBuffer.hasRemaining()) {
                flush()
            }
        }
    }

    private fun flush(): Error {
        val array = ByteArray(sendBuffer.position())
        sendBuffer.rewind()

        (array.indices).forEachIndexed { index, _ -> array[index] = sendBuffer.get() }
        val outputStream = socket.getOutputStream()
        try {
            outputStream.write(array)
        } catch (e: IOException) {
            logger.e(this, "Failed to send buffer: ${e.message}")
            return Error.TRANSMISSION_ERROR
        }
        sendBuffer.rewind()
        return Error.SUCCESS
    }

    fun disconnect() {
        socket.close()
    }

    override fun run() {
        if (!socket.isConnected)
            throw ConnectException("SrtEndpoint should be connected at this point")
    }

    override fun stop() {
        flush()
    }

    override fun close() {
        srt.cleanUp()
    }
}