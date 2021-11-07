/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.internal.bitbuffer

import com.github.thibaultbee.streampack.utils.Utils
import org.junit.Assert
import org.junit.Test

class BitBufferTest {
    @Test
    fun `put aligned ByteBuffer`() {
        val bitBuffer = BitBuffer.allocate(20)
        val randomByteBuffer = Utils.generateRandomBuffer(9)
        bitBuffer.put(0xFF.toByte())
        bitBuffer.put(randomByteBuffer)
        Assert.assertArrayEquals(
            bitBuffer.toByteBuffer().array().copyOfRange(1, randomByteBuffer.limit() + 1),
            randomByteBuffer.array()
        )
    }

    @Test
    fun `put aligned ByteArray`() {
        val bitBuffer = BitBuffer.allocate(20)
        val randomByteArray = Utils.generateRandomArray(9)
        bitBuffer.put(0xFF.toByte())
        bitBuffer.put(randomByteArray)
        Assert.assertArrayEquals(
            bitBuffer.toByteBuffer().array().copyOfRange(1, randomByteArray.size + 1),
            randomByteArray
        )
    }
}