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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags

import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByte
import java.nio.ByteBuffer
import kotlin.experimental.or

class FlvHeader(private val hasAudio: Boolean, private val hasVideo: Boolean) {
    companion object {
        private const val DATA_OFFSET = 9
        private const val HEADER_SIZE = DATA_OFFSET + 4 // 9 + 4 for PreviousTagSize0
    }

    fun write(buffer: ByteBuffer) {
        buffer.put(0x46) // 'F'
        buffer.put(0x4C) // 'L'
        buffer.put(0x56) // 'V'
        buffer.put(0x01) // Version
        buffer.put(hasVideo.toByte() or (hasAudio.toByte() shl 2).toByte())
        buffer.putInt(DATA_OFFSET)
        buffer.putInt(0) // PreviousTagSize0
    }

    fun write(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(HEADER_SIZE)

        write(buffer)

        buffer.rewind()

        return buffer
    }
}