/*
 * Copyright (C) 2024 Thibault B.
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

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Packet
import io.github.thibaultbee.streampack.core.elements.utils.extensions.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * Sink to write data to an [OutputStream]
 */
abstract class OutputStreamSink(private val coroutineContext: CoroutineContext = Dispatchers.IO) :
    AbstractSink() {
    protected var outputStream: OutputStream? = null

    private val _isOpen = MutableStateFlow(false)
    override val isOpen: StateFlow<Boolean> = _isOpen

    /**
     * Open an [OutputStream] to write data
     */
    abstract suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream

    override suspend fun openImpl(mediaDescriptor: MediaDescriptor) {
        outputStream = openOutputStream(mediaDescriptor)
        _isOpen.emit(true)
    }

    override fun configure(config: SinkConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        requireNotNull(outputStream) { "Open the sink before starting the stream" }
    }

    override suspend fun write(packet: Packet): Int {
        val outputStream = requireNotNull(outputStream) { "Open the sink before writing" }

        return withContext(coroutineContext) {
            val byteWritten = packet.buffer.remaining()
            outputStream.write(packet.buffer.toByteArray())
            byteWritten
        }
    }

    override suspend fun stopStream() {
        withContext(coroutineContext) {
            outputStream?.flush()
        }
    }

    override suspend fun close() {
        stopStream()
        try {
            withContext(coroutineContext) {
                outputStream?.close()
            }
        } catch (_: Throwable) {
            // Ignore
        } finally {
            outputStream = null
            _isOpen.emit(false)
        }
    }

    companion object {
        private const val TAG = "OutputStreamSink"
    }
}