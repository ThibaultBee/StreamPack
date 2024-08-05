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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks

import android.net.Uri
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.RandomAccessFile


class FileSink : ISink {
    private var file: RandomAccessFile? = null

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened

    override val metrics: Any
        get() = TODO("Not yet implemented")

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        require(!isOpened.value) { "FileSink is already opened" }
        require(mediaDescriptor.type.sinkType == MediaSinkType.FILE) { "MediaDescriptor must be a file" }

        file = openLocalFile(mediaDescriptor.uri)
        _isOpened.emit(true)
    }

    override fun configure(config: EndpointConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        require(file != null) { "Set a file before trying to write it" }
    }

    override suspend fun write(packet: io.github.thibaultbee.streampack.core.internal.data.Packet) {
        val file = file
        require(file != null) { "Set a file before trying to write it" }
        file.write(packet.buffer.toByteArray())
    }

    override suspend fun stopStream() {
        // Nothing to do
    }

    override suspend fun close() {
        try {
            file?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            file = null
            _isOpened.emit(false)
        }
    }

    companion object {

        private fun openLocalFile(uri: Uri): RandomAccessFile {
            return RandomAccessFile(uri.path, "rw")
        }
    }
}