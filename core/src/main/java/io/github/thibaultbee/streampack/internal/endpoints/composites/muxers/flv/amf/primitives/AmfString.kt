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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.amf.AmfParameter
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.amf.AmfType
import java.nio.ByteBuffer

class AmfString(private val s: String) : AmfParameter() {
    override val size: Int
        get() = 3 + s.length

    override fun encode(buffer: ByteBuffer) {
        if (s.length < 65536) {
            buffer.put(AmfType.STRING.value)
            AmfInt16(s.length).encode(buffer)
        } else {
            buffer.put(AmfType.LONG_STRING.value)
            AmfInt32(s.length).encode(buffer)
        }
        buffer.put(s.toByteArray())
    }
}