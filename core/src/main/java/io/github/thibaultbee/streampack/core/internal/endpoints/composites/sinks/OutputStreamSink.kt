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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks

import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
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
    ISink {
    protected var outputStream: OutputStream? = null

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened

    /**
     * Open an [OutputStream] to write data
     */
    abstract suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        require(!isOpened.value) { "OutputStreamSink is already opened" }

        outputStream = openOutputStream(mediaDescriptor)
        _isOpened.emit(true)
    }

    override fun configure(config: EndpointConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        require(outputStream != null) { "Open the sink before starting the stream" }
    }

    override suspend fun write(packet: Packet) : Int{
        val outputStream = outputStream
        require(outputStream != null) { "Open the sink before writing" }

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
        } catch (e: Exception) {
            // Ignore
        } finally {
            outputStream = null
            _isOpened.emit(false)
        }
    }
}