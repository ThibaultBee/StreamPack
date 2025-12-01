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

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.data.Packet
import io.github.thibaultbee.streampack.core.elements.interfaces.Configurable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendCloseable
import io.github.thibaultbee.streampack.core.elements.interfaces.SuspendStreamable
import kotlinx.coroutines.flow.StateFlow

interface ISinkInternal : ISink, Configurable<SinkConfiguration>, SuspendStreamable,
    SuspendCloseable {
    /**
     * Opens the endpoint.
     * @param mediaDescriptor the media descriptor
     */
    suspend fun open(mediaDescriptor: MediaDescriptor)

    /**
     * Writes a buffer to the [ISinkInternal].
     * @param packet buffer to write
     *
     * @return the number of bytes written
     */
    suspend fun write(packet: Packet): Int
}

interface ISink {
    /**
     * Whether if the endpoint is opened.
     * For example, if the file is opened for [FileSink].
     */
    val isOpenFlow: StateFlow<Boolean>

    /**
     * Metrics of the sink.
     */
    val metrics: Any
}