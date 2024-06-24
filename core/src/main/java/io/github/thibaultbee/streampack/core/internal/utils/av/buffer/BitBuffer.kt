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

import io.github.thibaultbee.streampack.core.internal.utils.extensions.toInt
import java.nio.ByteBuffer
import kotlin.math.ceil

open class BitBuffer(
    val buffer: ByteBuffer,
    var bitPosition: Int = buffer.position() * Byte.SIZE_BITS,
    val bitEnd: Int = buffer.limit() * Byte.SIZE_BITS - 1,
) {
    val hasRemaining: Boolean
        get() = bitRemaining > 0

    val bitRemaining: Int
        get() = bitEnd - bitPosition + 1

    val remaining: Int
        get() = ceil(bitRemaining.toFloat() / Byte.SIZE_BITS).toInt()

    init {
        require(bitPosition >= 0) { "bitPosition must be >= 0" }
        require(bitEnd >= 0) { "bitSize must be >= 0" }
        require(buffer.limit() * Byte.SIZE_BITS >= bitEnd) { "Buffer must be longer then buffer end" }
    }

    fun getBoolean(): Boolean {
        return get1Bit() == 1
    }

    private fun get1Bit(): Int {
        return getInt(1)
    }

    fun get(i: Int) = getLong(i).toByte()

    fun getShort(i: Int) = getLong(i).toShort()

    fun getInt(i: Int) = getLong(i).toInt()

    fun getLong(i: Int): Long {
        if (!hasRemaining) {
            throw IllegalStateException("No more bits to read")
        }

        val b = buffer[bitPosition / Byte.SIZE_BITS]
        val v = if (b < 0) b + 256 else b.toInt()
        val left = Byte.SIZE_BITS - bitPosition % Byte.SIZE_BITS
        var rc: Long
        if (i <= left) {
            rc =
                ((v shl (bitPosition % Byte.SIZE_BITS) and 0xFF) shr ((bitPosition % Byte.SIZE_BITS) + (left - i))).toLong()
            bitPosition += i
        } else {
            val then = i - left
            rc = getLong(left)
            rc = rc shl then
            rc += getLong(then)
        }

        buffer.position(ceil(bitPosition.toDouble() / Byte.SIZE_BITS).toInt())
        return rc
    }

    fun put(b: Boolean) {
        put(b.toInt(), 1)
    }

    fun put(i: Int, numBits: Int = Int.SIZE_BITS) {
        if (!hasRemaining) {
            throw IllegalStateException("No more bits to write")
        }

        val left = 8 - bitPosition % 8
        if (numBits <= left) {
            val currentPos = bitPosition / 8
            var current = buffer.get(currentPos).toInt()
            current = if (current < 0) current + 256 else current
            current += i shl (left - numBits)
            buffer.put(
                currentPos,
                (if (current > 127) current - 256 else current).toByte()
            )
            bitPosition += numBits
        } else {
            val bitsSecondWrite = numBits - left
            put(i shr bitsSecondWrite, left)
            put(i and (1 shl bitsSecondWrite) - 1, bitsSecondWrite)
        }

        buffer.position(ceil(bitPosition.toDouble() / Byte.SIZE_BITS).toInt())
    }

    fun put(b: Short, numBits: Int = Byte.SIZE_BITS) {
        put(if (b < 0) b.toInt() + 256 else b.toInt(), numBits)
    }

    fun put(b: Byte, numBits: Int = Byte.SIZE_BITS) {
        put(if (b < 0) b.toInt() + 256 else b.toInt(), numBits)
    }

    fun put(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            put(buffer.get())
        }
    }

    fun put(buffer: BitBuffer) {
        while (buffer.bitRemaining > Byte.SIZE_BITS) {
            put(buffer.get(Byte.SIZE_BITS), Byte.SIZE_BITS)
        }
        if (buffer.hasRemaining) {
            val bitRemaining = buffer.bitRemaining
            put(buffer.get(bitRemaining), bitRemaining)
        }
    }

    fun align(): Int {
        var left = 8 - bitPosition % 8
        if (left == 8) {
            left = 0
        }
        getLong(left)
        return left
    }
}