/*
 * Copyright (C) 2015 Sebastian Annies
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
package io.github.thibaultbee.streampack.core.internal.utils.av.video

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import java.nio.ByteBuffer

class H26XBitBuffer(
    buffer: ByteBuffer,
    initialPosition: Int = buffer.position()
) : BitBuffer(buffer, initialPosition) {

    fun readUE(): Int {
        val leadingZeros = numOfLeadingZeros()

        return if (leadingZeros > 0) {
            (1 shl leadingZeros) - 1 + getInt(leadingZeros)
        } else {
            0
        }
    }

    fun readSE(): Int {
        val value = readUE()
        val sign = (value and 0x1 shl 1) - 1
        return (value shr 1) + (value and 0x1) * sign
    }

    private fun numOfLeadingZeros(): Int {
        var leadingZeros = 0
        while (!getBoolean()) {
            leadingZeros++
        }
        return leadingZeros
    }
}