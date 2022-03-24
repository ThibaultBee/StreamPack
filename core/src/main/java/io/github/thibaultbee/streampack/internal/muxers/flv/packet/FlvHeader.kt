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
package io.github.thibaultbee.streampack.internal.muxers.flv.packet

import io.github.thibaultbee.streampack.internal.utils.shl
import io.github.thibaultbee.streampack.internal.utils.toByte
import java.nio.ByteBuffer
import kotlin.experimental.or

class FlvHeader(private val hasAudio: Boolean, private val hasVideo: Boolean) {
    companion object {
        private const val HEADER_SIZE = 13 // 9 + 4 for PreviousTagSize0
    }

    fun write(buffer: ByteBuffer) {
        buffer.put(0x46) // 'F'
        buffer.put(0x4C) // 'L'
        buffer.put(0x56) // 'V'
        buffer.put(0x01) // Version
        buffer.put(hasVideo.toByte() or (hasAudio.toByte() shl 2).toByte())
        buffer.putInt(HEADER_SIZE)
        buffer.putInt(0) // PreviousTagSize0
    }

    fun write(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(HEADER_SIZE)

        write(buffer)

        buffer.rewind()

        return buffer
    }
}