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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.containers

import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.AmfType
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.amf.primitives.AmfInt24
import java.nio.ByteBuffer

class AmfObject : AmfContainer() {
    override val size: Int
        get() = 4 /* 1 byte for start - 3 bytes for footer */ + parameters.sumOf {
            it.size
        }

    override fun encode(buffer: ByteBuffer) {
        buffer.put(AmfType.OBJECT.value)
        parameters.forEach { it.encode(buffer) }
        AmfInt24(AmfType.OBJECT_END.value.toInt()).encode(buffer)
    }
}