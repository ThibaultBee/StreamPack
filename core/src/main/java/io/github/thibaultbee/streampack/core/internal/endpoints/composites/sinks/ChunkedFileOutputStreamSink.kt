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

import androidx.core.net.toFile
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.internal.utils.ChunkedFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * Sink to write data to an [ChunkedFileOutputStream]
 */
class ChunkedFileOutputStreamSink(private val chunkSize: Int) : OutputStreamSink() {
    override val supportedSinkTypes: List<MediaSinkType> = listOf(MediaSinkType.FILE)
    
    private val listeners = mutableListOf<ChunkedFileOutputStream.Listener>()

    init {
        require(chunkSize > 0) { "Chunk size must be greater than 0" }
    }

    /**
     * Open an [OutputStream] to write data
     */
    override suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream {
        val file = mediaDescriptor.uri.toFile()
        file.deleteRecursively() // Clean
        val filesDir = if (file.isDirectory) {
            file
        } else {
            file.parentFile!!
        }
        require(filesDir.canWrite()) { "Cannot write in directory" }

        if (!filesDir.exists()) {
            require(withContext(Dispatchers.IO) {
                filesDir.mkdirs()
            })
        }

        val outputStream = if (file.isDirectory) {
            ChunkedFileOutputStream(filesDir, chunkSize = chunkSize)
        } else {
            ChunkedFileOutputStream(
                filesDir,
                chunkSize = chunkSize,
                chunkNameGenerator = { id -> "${file.nameWithoutExtension}_$id" })
        }

        listeners.forEach { outputStream.addListener(it) }
        listeners.clear()

        return outputStream
    }

    fun addListener(listener: ChunkedFileOutputStream.Listener) {
        if (outputStream == null) {
            listeners.add(listener)
        } else {
            (outputStream as ChunkedFileOutputStream).addListener(listener)
        }
    }

    fun removeListener(listener: ChunkedFileOutputStream.Listener) {
        if (outputStream == null) {
            listeners.remove(listener)
        } else {
            (outputStream as ChunkedFileOutputStream).removeListener(listener)
        }
    }

    fun removeListeners() {
        if (outputStream == null) {
            listeners.clear()
        } else {
            (outputStream as ChunkedFileOutputStream).removeListeners()
        }
    }
}