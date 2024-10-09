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
package io.github.thibaultbee.streampack.core.internal.utils.av.buffer

import io.github.thibaultbee.streampack.core.internal.utils.extensions.shr
import java.nio.ByteBuffer

class ByteBufferBitReader(var buffer: ByteBuffer) {
    private var nBit = 0
    private var currentByte: Byte
    private var nextByte: Byte

    init {
        currentByte = get()
        nextByte = get()
    }

    fun get(): Byte {
        return buffer.get()
    }

    fun read1Bit(): Int {
        if (nBit == 8) {
            advance()
        }
        val res = currentByte shr 7 - nBit and 1
        nBit++
        return res
    }

    private fun advance() {
        currentByte = nextByte
        nextByte = get()
        nBit = 0
    }

    fun readUE(): Int {
        var cnt = 0
        while (read1Bit() == 0) {
            cnt++
        }
        var res = 0
        if (cnt > 0) {
            res = ((1 shl cnt) - 1 + readNBit(cnt)).toInt()
        }
        return res
    }

    fun readNBit(n: Int): Long {
        require(n <= 64) { "Can not readByte more then 64 bit" }

        var value = 0L
        for (i in 0 until n) {
            value = value shl 1
            value = value or read1Bit().toLong()
        }
        return value
    }

    fun readBoolean(): Boolean {
        return read1Bit() != 0
    }

    fun readSE(): Int {
        var value = readUE()
        val sign = (value and 0x1 shl 1) - 1
        value = ((value shr 1) + (value and 0x1)) * sign
        return value
    }

    fun moreRBSPData(): Boolean {
        if (nBit == 8) {
            advance()
        }
        val tail = 1 shl 8 - nBit - 1
        val mask = (tail shl 1) - 1
        val hasTail = currentByte.toInt() and mask == tail
        return !(hasTail)
    }
}