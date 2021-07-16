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
package com.github.thibaultbee.streampack.internal.endpoints

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.logger.ILogger

/**
 * A fake endpoint for test purpose.
 */
class FakeEndpoint(val logger: ILogger) : IEndpoint {
    override fun startStream() {
        logger.d(this, "startStream called")
    }

    override fun configure(startBitrate: Int) {
        logger.d(this, "configure called with bitrate = $startBitrate")
    }

    override fun write(packet: Packet) {
        logger.d(this, "write called (packet size = ${packet.buffer.remaining()})")
    }

    override fun stopStream() {
        logger.d(this, "stopStream called")
    }

    override fun release() {
        logger.d(this, "release called")
    }
}