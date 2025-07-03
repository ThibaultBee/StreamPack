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

import android.content.Context
import android.net.Uri
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import kotlinx.coroutines.CoroutineDispatcher
import java.io.OutputStream

/**
 * An [OutputStreamSink] to write data to a content://.
 */
class ContentSink(private val context: Context, ioDispatcher: CoroutineDispatcher) :
    OutputStreamSink(ioDispatcher) {
    override val supportedSinkTypes: List<MediaSinkType> = listOf(MediaSinkType.CONTENT)

    override suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream {
        return openContent(context, mediaDescriptor.uri)
    }

    companion object {
        fun openContent(context: Context, uri: Uri): OutputStream {
            return context.contentResolver.openOutputStream(uri)
                ?: throw Exception("Cannot open content: $uri")
        }
    }
}