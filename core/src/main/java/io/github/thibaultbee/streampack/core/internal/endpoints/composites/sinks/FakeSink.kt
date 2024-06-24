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

import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.data.Packet
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A fake endpoint for test purpose.
 */
class FakeSink : ISink {
    private val _isOpened = MutableStateFlow(false)

    override val isOpened: StateFlow<Boolean> = _isOpened

    override val metrics: Any
        get() = TODO("Not yet implemented")


    override suspend fun open(mediaDescriptor: MediaDescriptor) {
        require(!isOpened.value) { "FakeSink is already opened" }

        Logger.d(TAG, "open called: $mediaDescriptor")
        _isOpened.emit(true)
    }

    override fun configure(config: EndpointConfiguration) {
        Logger.d(TAG, "configure called")
    }

    override suspend fun write(packet: Packet) {
        Logger.d(TAG, "write called (packet size = ${packet.buffer.remaining()})")
    }

    override suspend fun startStream() {
        Logger.d(TAG, "startStream called")
    }

    override suspend fun stopStream() {
        Logger.d(TAG, "stopStream called")
    }

    override suspend fun close() {
        Logger.d(TAG, "close called")
        _isOpened.emit(false)
    }

    companion object {
        private const val TAG = "FakeEndpoint"
    }
}