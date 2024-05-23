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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.tags

import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt24
import io.github.thibaultbee.streampack.internal.utils.extensions.shl
import java.nio.ByteBuffer

abstract class FlvTag(
    private var ts: Long,
    private val type: TagType,
    private val isEncrypted: Boolean = false /* Not supported yet */
) {
    protected abstract fun writeTagHeader(output: ByteBuffer)
    protected abstract val tagHeaderSize: Int
    protected abstract fun writeBody(output: ByteBuffer)
    protected abstract val bodySize: Int

    fun write(): ByteBuffer {
        val dataSize = tagHeaderSize + bodySize
        val flvTagSize = FLV_HEADER_TAG_SIZE + dataSize
        val buffer =
            ByteBuffer.allocateDirect(flvTagSize + 4) // 4 - PreviousTagSize

        // FLV Tag
        buffer.put((isEncrypted shl 5) or (type.value))
        buffer.putInt24(dataSize)
        val tsInMs = (ts / 1000).toInt() // to ms
        buffer.putInt24(tsInMs)
        buffer.put((tsInMs shr 24).toByte())
        buffer.putInt24(0) // Stream ID

        writeTagHeader(buffer)

        if (isEncrypted) {
            throw NotImplementedError("Filter/encryption is not implemented yet")
            // EncryptionTagHeader
            // FilterParams
        }

        writeBody(buffer)

        buffer.putInt(flvTagSize)

        buffer.rewind()

        return buffer
    }

    companion object {
        private const val FLV_HEADER_TAG_SIZE = 11
    }
}

enum class TagType(val value: Int) {
    AUDIO(8),
    VIDEO(9),
    SCRIPT(18),
}

