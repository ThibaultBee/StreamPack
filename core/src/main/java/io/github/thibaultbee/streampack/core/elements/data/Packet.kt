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
package io.github.thibaultbee.streampack.core.elements.data

import java.nio.ByteBuffer

/**
 * Packet internal representation.
 * A [Frame] is composed by multiple packets.
 */
open class Packet(
    /**
     * Contains data.
     */
    val buffer: ByteBuffer,

    /**
     * Frame timestamp in µs.
     */
    val ts: Long, // in µs

    /**
     * Packet data type
     */
    val type: PacketType = PacketType.UNKNOWN,
) {
    val isVideo = type == PacketType.VIDEO
    val isAudio = type == PacketType.AUDIO

    override fun toString(): String {
        return "Packet(buffer=$buffer, ts=$ts, type=$type)"
    }
}

enum class PacketType {
    /**
     * Video packet.
     */
    VIDEO,

    /**
     * Audio packet.
     */
    AUDIO,

    /**
     * Unknown packet.
     */
    UNKNOWN
}