/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.data

import java.nio.ByteBuffer

/**
 * SRT Packet internal representation.
 * A [Frame] is composed by multiple packets.
 */
class SrtPacket(
    /**
     * Contains data.
     */
    buffer: ByteBuffer,
    /**
     * [Boolean.true] if this is the first packet that describes a frame.
     */
    var isFirstPacketFrame: Boolean,
    /**
     * [Boolean.true] if this is the last packet that describes a frame.
     */
    var isLastPacketFrame: Boolean,
    /**
     * Frame timestamp in µs.
     */
    ts: Long, // in µs
) : io.github.thibaultbee.streampack.core.internal.data.Packet(buffer, ts)