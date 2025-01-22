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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks

import android.net.Uri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Packet
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.utils.extensions.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import kotlin.coroutines.CoroutineContext

class FileSink(private val coroutineContext: CoroutineContext = Dispatchers.IO) : AbstractSink() {
    override val supportedSinkTypes: List<MediaSinkType> = listOf(MediaSinkType.FILE)

    private var file: RandomAccessFile? = null

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow: StateFlow<Boolean> = _isOpenFlow

    override suspend fun openImpl(mediaDescriptor: MediaDescriptor) {
        file = openLocalFile(mediaDescriptor.uri)
        _isOpenFlow.emit(true)
    }

    override fun configure(config: SinkConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        requireNotNull(file) { "Set a file before trying to write it" }
    }

    override suspend fun write(packet: Packet): Int {
        val file = requireNotNull(file) { "Set a file before trying to write it" }

        return withContext(coroutineContext) {
            val byteWritten = packet.buffer.remaining()
            file.write(packet.buffer.toByteArray())
            byteWritten
        }
    }

    override suspend fun stopStream() {
        // Nothing to do
    }

    override suspend fun close() {
        try {
            withContext(coroutineContext) {
                file?.close()
            }
        } catch (_: Throwable) {
            // Ignore
        } finally {
            file = null
            _isOpenFlow.emit(false)
        }
    }

    companion object {
        private const val TAG = "FileSink"

        private fun openLocalFile(uri: Uri): RandomAccessFile {
            return RandomAccessFile(uri.path, "rw")
        }
    }
}