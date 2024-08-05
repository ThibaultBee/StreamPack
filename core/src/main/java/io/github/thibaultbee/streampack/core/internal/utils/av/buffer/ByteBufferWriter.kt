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
package io.github.thibaultbee.streampack.core.internal.utils.av.buffer

import io.github.thibaultbee.streampack.core.internal.utils.extensions.isAnnexB
import io.github.thibaultbee.streampack.core.internal.utils.extensions.isAvcc
import io.github.thibaultbee.streampack.core.internal.utils.extensions.removeStartCode
import io.github.thibaultbee.streampack.core.internal.utils.extensions.startCodeSize
import java.nio.ByteBuffer

abstract class ByteBufferWriter {
    abstract val size: Int

    open fun toByteBuffer(): ByteBuffer {
        val output = ByteBuffer.allocate(size)
        write(output)
        output.rewind()
        return output
    }

    abstract fun write(output: ByteBuffer)
}

/**
 * A class that acts as a passthrough for a [ByteBuffer].
 */
class PassthroughBufferWriter(private val buffer: ByteBuffer) : ByteBufferWriter() {
    override val size = buffer.remaining()

    override fun write(output: ByteBuffer) {
        output.put(buffer)
    }
}

/**
 * A class that convert a [ByteBuffer] to an AVCC format.
 */
class AVCCBufferWriter(private val buffer: ByteBuffer) : ByteBufferWriter() {
    private val isAvcc = buffer.isAvcc
    private val isAnnexB = buffer.isAnnexB

    override val size = computeSize()

    private fun computeSize(): Int {
        return if (isAvcc) {
            buffer.remaining()
        } else if (isAnnexB) {
            buffer.remaining() - buffer.startCodeSize + 4
        } else {
            throw IllegalArgumentException("Buffer must be in AVCC or AnnexB format")
        }
    }

    override fun write(output: ByteBuffer) {
        if (isAvcc) {
            output.put(buffer)
        } else if (isAnnexB) {
            val noStartCodeBuffer = buffer.removeStartCode()
            output.putInt(noStartCodeBuffer.remaining())
            output.put(noStartCodeBuffer)
        } else {
            throw IllegalArgumentException("Buffer must be in AVCC or AnnexB format")
        }
    }
}

