/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thibaultbee.streampack.endpoints

import com.github.thibaultbee.streampack.data.Packet
import com.github.thibaultbee.streampack.utils.ILogger
import java.io.File
import java.io.FileOutputStream


class FileWriter(val logger: ILogger) : IEndpoint {
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