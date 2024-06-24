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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.primitives

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.AmfParameter
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.amf.AmfType
import java.nio.ByteBuffer

class AmfNumber(private val d: Double) : AmfParameter() {
    override val size: Int
        get() = 9

    override fun encode(buffer: ByteBuffer) {
        buffer.put(AmfType.NUMBER.value)
        buffer.putLong(d.toRawBits())
    }
}