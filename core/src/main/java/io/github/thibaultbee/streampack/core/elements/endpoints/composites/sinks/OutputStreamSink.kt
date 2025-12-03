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
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.data.Packet
import io.github.thibaultbee.streampack.core.elements.utils.extensions.toByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Sink to write data to an [OutputStream]
 */
abstract class OutputStreamSink(protected val coroutineDispatcher: CoroutineDispatcher) :
    AbstractSink() {
    protected var outputStream: OutputStream? = null
        private set
    private val mutex = Mutex()

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    /**
     * Open an [OutputStream] to write data
     */
    abstract suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream

    override suspend fun openImpl(mediaDescriptor: MediaDescriptor) {
        withContext(coroutineDispatcher) {
            mutex.withLock {
                if (outputStream != null) {
                    throw IllegalStateException("Sink is already opened")
                }
                outputStream = openOutputStream(mediaDescriptor)
                _isOpenFlow.emit(true)
            }
        }
    }

    override fun configure(config: SinkConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        mutex.withLock {
            requireNotNull(outputStream) { "Open the sink before starting the stream" }
        }
    }

    protected open fun writeImpl(outputStream: OutputStream, buffer: ByteBuffer): Int {
        val byteWritten = buffer.remaining()
        outputStream.write(buffer.toByteArray())
        return byteWritten
    }

    override suspend fun write(packet: Packet): Int {
        return withContext(coroutineDispatcher) {
            mutex.withLock {
                val outputStream = requireNotNull(outputStream) { "Open the sink before writing" }
                writeImpl(outputStream, packet.buffer)
            }
        }
    }

    override suspend fun stopStream() {
        withContext(coroutineDispatcher) {
            mutex.withLock {
                try {
                    outputStream?.flush()
                } catch (_: Throwable) {
                    // Ignore
                }
            }
        }
    }

    override suspend fun close() {
        withContext(coroutineDispatcher) {
            mutex.withLock {
                try {
                    outputStream?.flush()
                } catch (_: Throwable) {
                    // Ignore
                }
                try {
                    outputStream?.close()
                } catch (_: Throwable) {
                    // Ignore
                } finally {
                    outputStream = null
                    _isOpenFlow.emit(false)
                }
            }
        }
    }

    companion object {
        private const val TAG = "OutputStreamSink"
    }
}