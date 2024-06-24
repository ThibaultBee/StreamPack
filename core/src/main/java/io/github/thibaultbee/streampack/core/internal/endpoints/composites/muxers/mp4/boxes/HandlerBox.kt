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

import io.github.thibaultbee.streampack.core.internal.utils.extensions.putString
import java.nio.ByteBuffer

class HandlerBox(private val type: HandlerType, private val name: String) : FullBox("hdlr", 0, 0) {
    override val size: Int = super.size + type.value.length + name.length + 17

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(0) // pre_defined
        output.putString(type.value)
        output.put(ByteArray(12)) // reserved
        output.putString(name)
        output.put(0.toByte()) // Null terminated string
    }

    enum class HandlerType(val value: String) {
        VIDEO("vide"),
        AUXILIARY_VIDEO("auxv"),
        SOUND("soun")
    }
}