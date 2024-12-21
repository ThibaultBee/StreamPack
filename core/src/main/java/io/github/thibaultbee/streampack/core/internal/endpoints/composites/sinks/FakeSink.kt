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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks

import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.internal.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A fake endpoint for test purpose.
 */
class FakeSink(override val supportedSinkTypes: List<MediaSinkType> = MediaSinkType.entries) :
    AbstractSink() {
    private val _isOpen = MutableStateFlow(false)
    override val isOpen: StateFlow<Boolean> = _isOpen

    override suspend fun openImpl(mediaDescriptor: MediaDescriptor) {
        Logger.d(TAG, "open called: $mediaDescriptor")
        _isOpen.emit(true)
    }

    override fun configure(config: SinkConfiguration) {
        Logger.d(TAG, "configure called")
    }

    override suspend fun write(packet: Packet): Int {
        Logger.d(TAG, "write called (packet size = ${packet.buffer.remaining()})")
        return packet.buffer.remaining()
    }

    override suspend fun startStream() {
        Logger.d(TAG, "startStream called")
    }

    override suspend fun stopStream() {
        Logger.d(TAG, "stopStream called")
    }

    override suspend fun close() {
        Logger.d(TAG, "close called")
        _isOpen.emit(false)
    }

    companion object {
        private const val TAG = "FakeEndpoint"
    }
}