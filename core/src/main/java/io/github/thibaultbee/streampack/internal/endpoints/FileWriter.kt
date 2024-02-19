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
package io.github.thibaultbee.streampack.internal.endpoints

import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class FileWriter(private val onErrorListener: OnErrorListener? = null) : IEndpoint {
    var file: File? = null
        set(value) {
            outputStream = if (value != null) {
                FileOutputStream(value, false)
            } else {
                outputStream?.close()
                null
            }
            field = value
        }

    var outputStream: OutputStream? = null

    override suspend fun startStream() {
        if (outputStream == null) {
            onErrorListener?.onError(StreamPackError("Set a file before trying to write it"))
        }
    }

    override fun configure(config: Int) {} // Nothing to configure

    override fun write(packet: Packet) {
        if (outputStream == null) {
            onErrorListener?.onError(StreamPackError("Set a file before trying to write it"))
        }
        try {
            outputStream!!.write(packet.buffer.toByteArray())
        } catch (e: IOException) {
            onErrorListener?.onError(StreamPackError(e)) ?: throw e
        }
    }

    override suspend fun stopStream() {
        try {
            outputStream!!.close()
        } catch (e: IOException) {
            onErrorListener?.onError(StreamPackError(e)) ?: throw e
        }
    }

    override fun release() {
    }
}