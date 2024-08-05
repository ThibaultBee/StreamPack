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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer

class ProgramConfigElement : BitBufferWriter() {
    override val bitSize: Int
        get() = TODO("Not yet implemented")

    override fun write(output: BitBuffer) {
        TODO("Not yet implemented")
    }

    companion object {
        fun parse(reader: BitBuffer): ProgramConfigElement {
            TODO("Not yet implemented")
        }
    }
}