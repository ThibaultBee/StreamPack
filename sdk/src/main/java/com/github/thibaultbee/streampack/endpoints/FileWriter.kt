package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.utils.Logger
import java.io.File
import java.io.FileOutputStream


class FileWriter(val logger: Logger? = null) : IEndpoint {
    var file: File = File.createTempFile("defaultFile", ".ts")
        set(value) {
            fileOutputStream = FileOutputStream(value, false)
            field = value
        }

    private var fileOutputStream = FileOutputStream(file)

    override fun startStream() {
    }

    override fun configure(startBitrate: Int) {} // Nothing to configure

    override fun write(packet: Packet) {
        fileOutputStream.channel.write(packet.buffer)
    }

    override fun stopStream() {
        fileOutputStream.channel.close()
    }

    override fun release() {
    }
}