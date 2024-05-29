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
package io.github.thibaultbee.streampack.internal.endpoints.composites.sinks

import android.content.Context
import android.net.Uri
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream


class ContentSink(private val context: Context) : ISink {
    private var outputStream: OutputStream? = null

    private val _isOpened = MutableStateFlow(false)
    override val isOpened: StateFlow<Boolean> = _isOpened

    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        mediaDescriptor as UriMediaDescriptor // Only UriMediaDescriptor is supported here
        require(!isOpened.value) { "ContentSink is already opened" }
        require(mediaDescriptor.sinkType == MediaSinkType.CONTENT) { "MediaDescriptor must be a content" }

        outputStream = openContent(context, mediaDescriptor.uri)
        _isOpened.emit(true)
    }

    override fun configure(config: EndpointConfiguration) {} // Nothing to configure

    override suspend fun startStream() {
        require(outputStream != null) { "Open the sink before starting the stream" }
    }

    override suspend fun write(packet: Packet) {
        require(outputStream != null) { "Open the sink before writing" }
        outputStream?.write(packet.buffer.toByteArray())
    }

    override suspend fun stopStream() {
        outputStream?.flush()
    }

    override suspend fun close() {
        stopStream()
        try {
            outputStream?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            outputStream = null
            _isOpened.emit(false)
        }
    }

    companion object {

        private fun openContent(context: Context, uri: Uri): OutputStream {
            return context.contentResolver.openOutputStream(uri)
                ?: throw Exception("Cannot open content: $uri")
        }
    }
}