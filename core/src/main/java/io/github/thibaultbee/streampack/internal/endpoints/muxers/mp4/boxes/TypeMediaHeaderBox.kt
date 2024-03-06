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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import java.nio.ByteBuffer

sealed class TypeMediaHeaderBox(
    type: String,
    version: Byte = 0,
    flags: Int
) : FullBox(type, version, flags)

class VideoMediaHeaderBox(
    private val graphicsMode: Short = 0,
    private val opColor: ShortArray = ShortArray(3)
) : TypeMediaHeaderBox("vmhd", 0, 1) {
    init {
        require(opColor.size == 3) { "opColor must have 3 elements" }
    }

    override val size: Int = super.size + 8

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putShort(graphicsMode) // graphics mode
        opColor.forEach { output.putShort(it) } // op color
    }
}

class SoundMediaHeaderBox(
    private val balance: Short = 0,
) : TypeMediaHeaderBox("smhd", 0, 0) {
    override val size: Int = super.size + 4

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putShort(balance)
        output.putShort(0) // reserved
    }
}