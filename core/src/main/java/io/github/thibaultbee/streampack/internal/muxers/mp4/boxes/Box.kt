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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.putInt24
import io.github.thibaultbee.streampack.internal.utils.putString
import java.nio.ByteBuffer

sealed class Box(private val type: String, private val isCompact: Boolean = true) {
    open val size: Int = 8 + if (!isCompact) {
        8
    } else {
        0
    } + if (type == "uuid") {
        128
    } else {
        0
    }

    open fun write(buffer: ByteBuffer) {
        if (isCompact) {
            buffer.putInt(size)
        } else {
            buffer.putInt(1)
        }

        buffer.putString(type)
        if (!isCompact) {
            throw NotImplementedError("Large size not implemented yet")
        }
        if (type == "uuid") {
            throw NotImplementedError("UUID not implemented yet")
        }
    }

    fun write(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(size)

        write(buffer)

        buffer.rewind()

        return buffer
    }
}

abstract class FullBox(
    type: String,
    protected val version: Byte,
    private val flags: Int,
    isCompact: Boolean = true
) : Box(type, isCompact) {
    override val size: Int = super.size + 4

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.put(version)
        buffer.putInt24(flags)
    }
}
