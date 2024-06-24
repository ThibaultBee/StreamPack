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
import java.io.OutputStream

/**
 * Sink to write data to an [ChunkedFileOutputStream]
 */
class ChunkedFileOutputStreamSink(private val chunkSize: Int) : OutputStreamSink() {
    override val metrics: Any
        get() = TODO("Not yet implemented")

    init {
        require(chunkSize > 0) { "Chunk size must be greater than 0" }
    }

    /**
     * Open an [OutputStream] to write data
     */
    override suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream {
        require(mediaDescriptor.type.sinkType == MediaSinkType.FILE) { "MediaDescriptor must be a file" }
        val filesDir = mediaDescriptor.uri.toFile()
        require(filesDir.isDirectory) { "URI must be a directory" }
        require(filesDir.canWrite()) { "Cannot write in directory" }

        return ChunkedFileOutputStream(filesDir, chunkSize = chunkSize)
    }

    fun addListener(listener: ChunkedFileOutputStream.Listener) {
        (outputStream as ChunkedFileOutputStream).addListener(listener)
    }

    fun removeListener(listener: ChunkedFileOutputStream.Listener) {
        (outputStream as ChunkedFileOutputStream).removeListener(listener)
    }
}