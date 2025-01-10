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

import androidx.core.net.toFile
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import java.io.OutputStream

/**
 * [OutputStreamSink] tests on a [File].
 */
class OutputStreamSinkFileTest : AbstractLocalSinkTest(object : OutputStreamSink() {
    override suspend fun openOutputStream(mediaDescriptor: MediaDescriptor): OutputStream {
        return mediaDescriptor.uri.toFile().outputStream()
    }

    override val supportedSinkTypes: List<MediaSinkType> = listOf(MediaSinkType.FILE)
})
