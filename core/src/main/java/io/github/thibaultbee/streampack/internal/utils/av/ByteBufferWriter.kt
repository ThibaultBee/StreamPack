/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.utils.av

import java.nio.ByteBuffer
import kotlin.math.ceil

abstract class ByteBufferWriter {
    open val size: Int
        get() = ceil(bitSize.toFloat() / Byte.SIZE_BITS).toInt()
    open val bitSize: Int
        get() = size * Byte.SIZE_BITS

    fun toByteBuffer(): ByteBuffer {
        val output = ByteBuffer.allocate(size)
        write(output)
        output.rewind()
        return output
    }

    abstract fun write(output: ByteBuffer)
}

/**
 * For sub classes of a [ByteBufferWriter] that need to write a [ByteBuffer] to a [BitBuffer]
 */
abstract class BitBufferWriter : ByteBufferWriter() {
    override fun write(output: ByteBuffer) {
        val writer = BitBuffer(output)
        write(writer)
    }

    abstract fun write(writer: BitBuffer)

    fun toBitBuffer(): BitBuffer {
        val buffer = ByteBuffer.allocate(size)
        val output = BitBuffer(buffer)
        write(output)
        buffer.rewind()
        return output
    }
}
