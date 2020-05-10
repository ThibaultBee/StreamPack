package com.github.thibaultbee.srtstreamer.transmission

import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import com.github.thibaultbee.srtwrapper.Srt
import com.github.thibaultbee.srtwrapper.enums.SockOpt
import com.github.thibaultbee.srtwrapper.enums.SockStatus
import com.github.thibaultbee.srtwrapper.enums.Transtype
import com.github.thibaultbee.srtwrapper.models.Error.Companion.clearLastError
import com.github.thibaultbee.srtwrapper.models.Error.Companion.getLastErrorMessage
import com.github.thibaultbee.srtwrapper.models.Socket
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
        if (tmpSocket.setSockFlag(SockOpt.SNDSYN, true) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to SNDSYN: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }

        if (tmpSocket.setSockFlag(SockOpt.RCVSYN, true) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to RCVSYN: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }

        if (tmpSocket.setSockFlag(SockOpt.TRANSTYPE, Transtype.LIVE) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to RCVSYN: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }

        if (tmpSocket.setSockFlag(SockOpt.MAXBW, 0) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to MAXBW: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }
/*
        if (tmpSocket.setSockFlag(SockOpt.TSBPDMODE, false) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to MAXBW: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }*/

        if (tmpSocket.setSockFlag(SockOpt.INPUTBW, 1000000) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to MAXBW: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }

        if (tmpSocket.setSockFlag(SockOpt.OHEADBW, 25) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to set sock flag to MAXBW: ${getErrorMessage()}")
            return Error.INVALID_OPERATION
        }

        if (tmpSocket.connect(ip, port) != 0) {
            tmpSocket.close()
            logger.e(this, "Failed to connect to $ip:$port: ${getErrorMessage()}")
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
            if (socket!!.getSockState() != SockStatus.CONNECTED) {
                disconnect()
            }
            return Error.TRANSMISSION_ERROR
        }
        sendBuffer.rewind()
        return Error.SUCCESS
    }

    fun disconnect() {
        socket?.close()
        socket = null
        srt.cleanUp()
    }

    fun isConnected(): Boolean {
        return socket?.getSockState() == SockStatus.CONNECTED
    }

    private fun getErrorMessage(): String {
        val message = getLastErrorMessage()
        clearLastError()
        return message
    }
}