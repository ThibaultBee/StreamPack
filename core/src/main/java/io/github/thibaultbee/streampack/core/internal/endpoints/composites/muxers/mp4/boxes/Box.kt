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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putInt24
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putString
import java.nio.ByteBuffer

sealed class Box(private val type: String, private val isCompact: Boolean = true) :
    ByteBufferWriter() {
    override val size: Int = 8 + if (!isCompact) {
        8
    } else {
        0
    } + if (type == "uuid") {
        128
    } else {
        0
    }

    override fun write(output: ByteBuffer) {
        if (isCompact) {
            output.putInt(size)
        } else {
            output.putInt(1)
        }

        output.putString(type)
        if (!isCompact) {
            throw NotImplementedError("Large size not implemented yet")
        }
        if (type == "uuid") {
            throw NotImplementedError("UUID not implemented yet")
        }
    }
}

abstract class FullBox(
    type: String,
    protected val version: Byte,
    private val flags: Int,
    isCompact: Boolean = true
) : Box(type, isCompact) {
    override val size: Int = super.size + 4

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(version)
        output.putInt24(flags)
    }
}
