package com.github.thibaultbee.srtstreamer.publisher

import com.github.thibaultbee.srtdroid.Srt
import com.github.thibaultbee.srtdroid.enums.SockOpt
import com.github.thibaultbee.srtdroid.enums.Transtype
import com.github.thibaultbee.srtdroid.models.Socket
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.io.IOException
import java.nio.ByteBuffer

class SrtPublisher(val logger: Logger): EventHandlerManager() {
    private val srt = Srt()
    private var socket = Socket()
    private lateinit var sendBuffer: ByteBuffer

    init {
        srt.startUp()
    }

    fun connect(ip: String, port: Int): Error {
        socket = Socket()
        try {
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

        sendBuffer = ByteBuffer.allocate(socket.getSockFlag(SockOpt.PAYLOADSIZE) as Int)
        sendBuffer.rewind()
        return Error.SUCCESS
    }

    fun write(buffer: ByteBuffer): Error {
        // Read bytebuffer
        while (buffer.hasRemaining()) {
            sendBuffer.put(buffer)
            if (!sendBuffer.hasRemaining()) {
                flush()
            }
        }

        return Error.SUCCESS
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

    fun release() {
        srt.cleanUp()
    }

    fun isConnected(): Boolean {
        return socket.isConnected
    }
}