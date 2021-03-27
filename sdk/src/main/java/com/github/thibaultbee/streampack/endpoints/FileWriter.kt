package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.utils.Logger
import java.io.File
import java.io.FileOutputStream


class FileWriter(val logger: Logger? = null, file: File) : IEndpoint {
    private val fileOutputStream = FileOutputStream(file, false)

    override fun startStream() {
        if (!fileOutputStream.fd.valid()) {
            throw InterruptedException("FileWriter file descriptor is invalid")
        }
    }

    override fun configure(bitrate: Int) {} // Nothing to configure

    override fun write(packet: Packet) {
        fileOutputStream.channel.write(packet.buffer)
    }

    override fun stopStream() {}

    override fun release() {
        fileOutputStream.channel.close()
    }
}