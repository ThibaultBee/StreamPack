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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf

import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfBoolean
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfInt16
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfInt32
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfNumber
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfString
import java.io.IOException
import java.nio.ByteBuffer

abstract class AmfParameter {
    abstract val size: Int

    abstract fun encode(buffer: ByteBuffer)

    fun encode(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(size)
        encode(buffer)
        buffer.rewind()
        return buffer
    }

    companion object {
        fun build(v: Any): AmfParameter {
            return when (v) {
                is Boolean -> AmfBoolean(v)
                is Short -> AmfInt16(v)
                is Int -> AmfInt32(v)
                is Double -> AmfNumber(v)
                is String -> AmfString(v)
                else -> throw IOException("Can't build an AmfParameter for ${v::class.java.simpleName}")
            }
        }
    }
}

enum class AmfType(val value: Byte) {
    NUMBER(0x00),
    BOOLEAN(0x01),
    STRING(0x02),
    OBJECT(0x03),
    NULL(0x05),
    ECMA_ARRAY(0x08),
    OBJECT_END(0x09),
    STRICT_ARRAY(0x0A),
    DATE(0x0B),
    LONG_STRING(0x0C),
}