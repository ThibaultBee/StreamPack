package com.github.thibaultbee.srtstreamer.publisher

import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class FilePublisher(val logger: Logger? = null, file: File) : EventHandlerManager() {
    private val fileOutputStream = FileOutputStream(file, false)

    fun write(buffer: ByteBuffer): Error {
        fileOutputStream.channel.write(buffer)

        return Error.SUCCESS
    }

    fun release() {
        fileOutputStream.channel.close()
    }
}