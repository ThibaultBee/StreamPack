package com.github.thibaultbee.srtstreamer.transmission

import com.github.thibaultbee.srtdroid.Srt
import com.github.thibaultbee.srtdroid.enums.SockOpt
import com.github.thibaultbee.srtdroid.enums.SockStatus
import com.github.thibaultbee.srtdroid.enums.Transtype
import com.github.thibaultbee.srtdroid.models.Error.Companion.clearLastError
import com.github.thibaultbee.srtdroid.models.Error.Companion.lastErrorMessage
import com.github.thibaultbee.srtdroid.models.Socket
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.io.IOException
import java.nio.ByteBuffer

class SrtPublisher(val logger: Logger): EventHandlerManager() {
    private val srt: Srt = Srt()
    private var socket: Socket? = null
    private var sendBuffer = ByteBuffer.allocateDirect(MAX_PAYLOAD_SIZE)

    companion object {
        val MAX_PAYLOAD_SIZE = 1316
    }

    fun connect(ip: String, port: Int): Error {
        srt.startUp()
        val tmpSocket = Socket()
        try {
            tmpSocket.setSockFlag(SockOpt.SNDSYN, true)
            tmpSocket.setSockFlag(SockOpt.RCVSYN, true)
            tmpSocket.setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE)
            tmpSocket.setSockFlag(SockOpt.MAXBW, 0)
            // tmpSocket.setSockFlag(SockOpt.TSBPDMODE, false)
            tmpSocket.setSockFlag(SockOpt.INPUTBW, 1000000)
            tmpSocket.setSockFlag(SockOpt.OHEADBW, 25)
        } catch (e: IOException) {
            tmpSocket.close()
            logger.e(this, "Failed to configure for connection with $ip:$port: ${e.message}")
            return Error.CONFIGURATION_ERROR
        }

        try {
            tmpSocket.connect(ip, port)
        } catch (e: IOException) {
            tmpSocket.close()
            logger.e(this, "Failed to connect to $ip:$port: ${e.message}")
            return Error.CONNECTION_ERROR
        }

        sendBuffer.rewind()
        socket = tmpSocket
        return Error.SUCCESS
    }

    fun write(buffer: ByteBuffer): Error {
        if (socket == null) {
            return Error.BAD_STATE
        }

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
        /*
         * ByteBuffer API getArray(), add a header. It is inappropriate to use where we need the
         * exact size. Instead we iterate through the ByteBuffer
         */
        (array.indices).forEachIndexed { index, _ -> array[index] = sendBuffer.get() }
        if (socket!!.send(array) < 0) {
            logger.e(this, "Failed to send buffer: ${getErrorMessage()}")
            if (socket!!.sockState != SockStatus.CONNECTED) {
                disconnect()
            }
            return Error.TRANSMISSION_ERROR
        }
        sendBuffer.rewind()
        return Error.SUCCESS
    }

    fun disconnect() {
        socket.close()
        socket = null
        srt.cleanUp()
    }

    fun isConnected(): Boolean {
        return socket.sockState == SockStatus.CONNECTED
    }

    private fun getErrorMessage(): String {
        val message = lastErrorMessage
        clearLastError()
        return message
    }
}