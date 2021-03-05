package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.utils.Logger
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class FileWriter(val logger: Logger? = null, file: File) : IEndpoint {
    private val fileOutputStream = FileOutputStream(file, false)

    override fun startStream() {
        if (!fileOutputStream.fd.valid()) {
            throw InterruptedException("FileWriter file descriptor is invalid")
        }
    }

    override fun write(buffer: ByteBuffer) {
        fileOutputStream.channel.write(buffer)
    }

    override fun stopStream() {}

    override fun release() {
        fileOutputStream.channel.close()
    }
}