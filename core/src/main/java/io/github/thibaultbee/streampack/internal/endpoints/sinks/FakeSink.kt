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
package io.github.thibaultbee.streampack.internal.endpoints.sinks

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.logger.Logger

/**
 * A fake endpoint for test purpose.
 */
class FakeSink : ISink {
    override fun configure(config: Int) {
        Logger.d(TAG, "configure called with bitrate = $config")
    }

    override fun write(packet: Packet) {
        Logger.d(TAG, "write called (packet size = ${packet.buffer.remaining()})")
    }

    override suspend fun startStream() {
        Logger.d(TAG, "startStream called")
    }
    
    override suspend fun stopStream() {
        Logger.d(TAG, "stopStream called")
    }

    override fun release() {
        Logger.d(TAG, "release called")
    }

    companion object {
        private const val TAG = "FakeEndpoint"
    }
}