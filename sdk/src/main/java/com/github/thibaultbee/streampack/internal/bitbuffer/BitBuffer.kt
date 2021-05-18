/**
 * MIT License
 *
 * Copyright (c) 2018 Jacob G.
 * Copyright (c) 2021 Thibault Beyou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.thibaultbee.streampack.internal.bitbuffer

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * A data type similar to [ByteBuffer], but can read/write bits as well as `byte`s to improve
 * throughput and allow for optional compression.
 *
 * @author Jacob G.
 * @version February 24, 2018
 */
class BitBuffer private constructor(buffer: ByteBuffer) {
    companion object {
        /**
         * The mask used when writing/reading bits.
         */
        private val MASKS = LongArray(Long.SIZE_BITS + 1)

        /**
         * Allocates a new [BitBuffer] backed by a [ByteBuffer].
         *
         * @param capacity the capacity of the [BitBuffer] in `byte`s.
         * @return a [BitBuffer] allocated with the specified capacity.
         */
        fun allocate(capacity: Int): BitBuffer {
            return BitBuffer(ByteBuffer.allocate(capacity + Long.SIZE_BYTES))
        }

        /**
         * Allocates a new [BitBuffer] backed by a [ByteBuffer].
         *
         * @param bitCapacity the capacity of the [BitBuffer] in `bit`s.
         * @return a [BitBuffer] allocated with the specified capacity.
         */
        fun allocate(bitCapacity: Long): BitBuffer {
            return allocate(((bitCapacity + 7) / 8).toInt())
        }

        /**
         * Allocates a new [BitBuffer] backed by a **direct** [ByteBuffer].
         * <br></br><br></br>
         * This should be used over [.allocate] when the [BitBuffer] will be used for I/O (files,
         * networking, etc.).
         *
         * @param capacity the capacity of the [BitBuffer] in `byte`s.
         * @return this [BitBuffer] to allow for the convenience of method-chaining.
         */
        fun allocateDirect(capacity: Int): BitBuffer {
            return BitBuffer(ByteBuffer.allocateDirect(capacity + Long.SIZE_BYTES))
        }

        /**
         * Allocates a new [BitBuffer] backed by a **direct** [ByteBuffer].
         * <br></br><br></br>
         * This should be used over [.allocate] when the [BitBuffer] will be used for I/O (files,
         * networking, etc.).
         *
         * @param bitCapacity the capacity of the [BitBuffer] in `byte`s.
         * @return this [BitBuffer] to allow for the convenience of method-chaining.
         */
        fun allocateDirect(bitCapacity: Long): BitBuffer {
            return allocateDirect(((bitCapacity + 7) / 8).toInt())
        }

        /**
         * Initialize the mask to its respective values.
         */
        init {
            for (i in MASKS.indices) {
                MASKS[i] = BigInteger.valueOf(2).pow(i).subtract(BigInteger.ONE).toLong()
            }
        }
    }

    /**
     * The backing [ByteBuffer].
     */
    private val buffer = buffer.order(ByteOrder.BIG_ENDIAN)

    /**
     * The number of bits available within `cache`.
     */
    private var remainingBits = Long.SIZE_BITS

    /**
     * The *cache* used when writing and reading bits.
     */
    private var cache: Long = 0

    /**
     * Writes `value` to this [BitBuffer] using `numBits` bits.
     *
     * @param value   the value to write.
     * @param numBits the amount of bits to use when writing `value`.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    private fun putBits(value: Long, numBits: Int): BitBuffer {
        // If the value that we're writing is too large to be placed entirely in the cache, then we need to place as
        // much as we can in the cache (the least significant bits), flush the cache to the backing ByteBuffer, and
        // place the rest in the cache.
        if (remainingBits < numBits) {
            val upperHalfBits = numBits - remainingBits
            cache =
                cache or ((value shr remainingBits) and MASKS[remainingBits])
            buffer.putLong(cache)
            remainingBits = Long.SIZE_BITS - upperHalfBits
            cache = value and MASKS[upperHalfBits] shl remainingBits
        } else {
            cache = cache or (value and MASKS[numBits] shl (remainingBits - numBits))
            remainingBits -= numBits
        }
        return this
    }

    /**
     * Write current cache and clean it
     */
    private fun flushCache() {
        buffer.putLong(cache)
        cache = 0
        remainingBits = Long.SIZE_BITS
    }

    /**
     * Writes a bit to this [BitBuffer].
     *
     * @param b          the `boolean` to write.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(b: Boolean): BitBuffer {
        return putBits(if (b) 1 else 0.toLong(), 1)
    }

    /**
     * Writes a value to this [BitBuffer] using [Byte.SIZE_BITS] bits.
     *
     * @param b the `byte` to write.
     * @param numBits number of bits to write
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(b: Byte, numBits: Int = Byte.SIZE_BITS): BitBuffer {
        return putBits(b.toLong(), numBits)
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Char.SIZE_BITS] bits.
     *
     * @param c the `char` to write.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        c: Char,
        numBits: Int = Char.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return putBits(
            (if (order == ByteOrder.BIG_ENDIAN) Character.reverseBytes(c) else c).toLong(),
            numBits
        )
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Double.SIZE_BITS] bits.
     *
     * @param d the `double` to write.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        d: Double,
        numBits: Int = Double.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return put(java.lang.Double.doubleToRawLongBits(d), numBits, order)
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Float.SIZE_BITS] bits.
     *
     * @param f the `float` to write.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        f: Float,
        numBits: Int = Float.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return put(java.lang.Float.floatToRawIntBits(f), numBits, order)
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Int.SIZE_BITS] bits.
     *
     * @param i the `int` to write.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        i: Int,
        numBits: Int = Int.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return putBits(
            (if (order == ByteOrder.BIG_ENDIAN) Integer.reverseBytes(i) else i).toLong(),
            numBits
        )
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Long.SIZE_BITS] bits.
     *
     * @param l the `int` to write.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        l: Long,
        numBits: Int = Long.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return putBits(
            if (order == ByteOrder.BIG_ENDIAN) java.lang.Long.reverseBytes(l) else l,
            numBits
        )
    }

    /**
     * Writes a value with [ByteOrder.BIG_ENDIAN] order to this [BitBuffer] using [Short.SIZE_BITS] bits.
     *
     * @param s the `short` to write as an `int` for ease-of-use, but internally down-casted to a `short`.
     * @param numBits number of bits to write
     * @param order byte order
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(
        s: Short,
        numBits: Int = Short.SIZE_BITS,
        order: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): BitBuffer {
        return putBits(
            (if (order == ByteOrder.BIG_ENDIAN) java.lang.Short.reverseBytes(s) else s).toLong(),
            numBits
        )
    }

    /**
     * Writes a String to this [BitBuffer] using [Byte.SIZE_BITS] bits for each `byte`.
     *
     * @param src the string to write.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(s: String): BitBuffer {
        for (c in s.toByteArray(StandardCharsets.UTF_8)) {
            put(c)
        }
        return this
    }

    /**
     * Writes an array of `byte`s to this [BitBuffer] using [Byte.SIZE_BITS] bits for each `byte`.
     *
     * @param src the array of `byte`s to write.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(src: ByteArray): BitBuffer {
        if (remainingBits % Byte.SIZE_BITS == 0) {
            val numRewindBytes = remainingBits / 8
            flushCache()
            buffer.position(buffer.position() - numRewindBytes)
            buffer.put(src)
        } else {
            for (b in src) {
                put(b)
            }
        }
        return this
    }

    /**
     * Writes a ByteBuffer to this [BitBuffer] using [Byte.SIZE_BITS] bits for each `byte`.
     *
     * @param src the array of `byte`s to write.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun put(src: ByteBuffer): BitBuffer {
        if (remainingBits % Byte.SIZE_BITS == 0) {
            val numRewindBytes = remainingBits / 8
            flushCache()
            buffer.position(buffer.position() - numRewindBytes)
            buffer.put(src)
        } else {
            while (src.hasRemaining()) {
                put(src.get())
            }
        }
        return this
    }

    /**
     * Given the specified `maxValue`, this method writes the specified value to this [BitBuffer]
     * using the optimal amount of bits, thus providing free compression.
     *
     * @param value    the value to write to this [BitBuffer].
     * @param maxValue the maximum possible value that `value` can be; a lower `maxValue` results in a
     * better compression ratio.
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     * @throws IllegalArgumentException if `maxValue` is negative or if the absolute value of `value` is
     * greater than `maxValue`.
     */
    @Throws(IllegalArgumentException::class)
    fun putValue(value: Long, maxValue: Long): BitBuffer {
        require(maxValue >= 0) { "maxValue must be positive!" }
        require(abs(value) <= maxValue) { "value must be less than or equal to maxValue!" }
        val numBits = Long.SIZE_BITS - maxValue.countLeadingZeroBits() + 1
        return putBits(value, numBits)
    }

    /**
     * After a series of relative `put` operations, flip the *cache* to prepare for a series of relative
     * `get` operations.
     *
     * @return this [BitBuffer] to allow for the convenience of method-chaining.
     */
    fun flip(): BitBuffer {
        // Put the cache into the buffer if applicable.
        if (remainingBits != Long.SIZE_BITS) {
            buffer.putLong(cache)
        }

        // Reset the buffer's position and limit.
        buffer.clear()

        // Set remainingBits to 0 so that, on the next call to getBits, the cache will be reset.
        remainingBits = 0
        return this
    }

    /**
     * Reads the next `numBits` bits and composes a `long` that can be down-casted to other primitive types.
     *
     * @param numBits the amount of bits to read.
     * @return a `long` value at the [BitBuffer]'s current position.
     */
    private fun getBits(numBits: Int): Long {
        var value: Long
        if (remainingBits < numBits) {
            value = cache and MASKS[remainingBits]
            cache = buffer.long
            val difference = numBits - remainingBits
            value = value or (cache and MASKS[difference] shl remainingBits)
            cache = cache shr difference
            remainingBits = Long.SIZE_BITS - difference
        } else {
            value = cache and MASKS[numBits]
            cache = cache shr numBits
            remainingBits -= numBits
        }
        return value
    }

    /**
     * Reads a bit from this
     * [BitBuffer] and composes a `boolean`.
     *
     * @return `true` if the value read is not equal to `0`, otherwise `false`.
     */
    fun getBoolean(): Boolean {
        return getBits(1) != 0L
    }

    /**
     * Reads [Byte.SIZE_BITS] bits from this [BitBuffer] and composes a `byte`.
     *
     * @return A `byte`.
     */
    val byte: Byte
        get() = getBits(Byte.SIZE_BITS).toByte()

    /**
     * Reads the specified amount of `byte`s from this [BitBuffer] into an array of `byte`s.
     *
     * @param n the number of `byte`s to read.
     * @return an array of `byte`s of length `n` that contains `byte`s read from this [BitBuffer].
     */
    fun getBytes(n: Int): ByteArray {
        val array = ByteArray(n)
        for (i in array.indices) {
            array[i] = byte
        }
        return array
    }

    /**
     * Reads [Char.SIZE_BITS] bits from this [BitBuffer] and composes a `char` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return A `char`.
     * @see .getChar
     */
    val char: Char
        get() = getChar(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Char.SIZE_BITS] bits from this [BitBuffer] and composes a `char` with the specified
     * [ByteOrder].
     *
     * @return A `char`.
     */
    fun getChar(order: ByteOrder): Char {
        val value = getBits(Char.SIZE_BITS).toChar()
        return if (order == ByteOrder.BIG_ENDIAN) Character.reverseBytes(value) else value
    }

    /**
     * Reads [Double.SIZE_BITS] bits from this [BitBuffer] and composes a `double` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return A `double`.
     * @see .getDouble
     */
    val double: Double
        get() = getDouble(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Double.SIZE_BITS] bits from this [BitBuffer] and composes a `double` with the specified
     * [ByteOrder].
     *
     * @return A `double`.
     * @see .getLong
     */
    fun getDouble(order: ByteOrder): Double {
        return java.lang.Double.longBitsToDouble(getLong(order))
    }

    /**
     * Reads [Float.SIZE_BITS] bits from this [BitBuffer] and composes a `float` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return A `float`.
     * @see .getFloat
     */
    val float: Float
        get() = getFloat(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Float.SIZE_BITS] bits from this [BitBuffer] and composes a `float` with the specified
     * [ByteOrder].
     *
     * @return A `float`.
     * @see .getFloat
     */
    fun getFloat(order: ByteOrder): Float {
        return java.lang.Float.intBitsToFloat(getInt(order))
    }

    /**
     * Reads [Int.SIZE_BITS] bits from this [BitBuffer] and composes an `int` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return An `int`.
     * @see .getInt
     */
    val int: Int
        get() = getInt(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Int.SIZE_BITS] bits from this [BitBuffer] and composes an `int` with the specified
     * [ByteOrder].
     *
     * @return An `int`.
     */
    fun getInt(order: ByteOrder): Int {
        val value = getBits(Int.SIZE_BITS).toInt()
        return if (order == ByteOrder.BIG_ENDIAN) Integer.reverseBytes(value) else value
    }

    /**
     * Reads [Long.SIZE_BITS] bits from this [BitBuffer] and composes an `int` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return A `long`.
     * @see .getLong
     */
    val long: Long
        get() = getLong(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Long.SIZE_BITS] bits from this [BitBuffer] and composes a `long` with the specified
     * [ByteOrder].
     *
     * @return A `long`.
     */
    fun getLong(order: ByteOrder): Long {
        val value = getBits(Long.SIZE_BITS)
        return if (order == ByteOrder.BIG_ENDIAN) java.lang.Long.reverseBytes(value) else value
    }

    /**
     * Reads [Short.SIZE_BITS] bits from this [BitBuffer] and composes a `short` with
     * [ByteOrder.BIG_ENDIAN] order.
     *
     * @return A `short`.
     * @see .getShort
     */
    val short: Short
        get() = getShort(ByteOrder.LITTLE_ENDIAN)

    /**
     * Reads [Short.SIZE_BITS] bits from this [BitBuffer] and composes a `short` with the specified
     * [ByteOrder].
     *
     * @return A `short`.
     */
    fun getShort(order: ByteOrder): Short {
        val value = getBits(Short.SIZE_BITS).toShort()
        return if (order == ByteOrder.BIG_ENDIAN) java.lang.Short.reverseBytes(value) else value
    }

    /**
     * Given the specified `maxValue`, this method reads a value from this [BitBuffer] using the optimal
     * amount of bits, thus providing free compression.
     * <br></br><br></br>
     * The value of `maxValue` should be the same as what was used when calling [.putValue].
     *
     * @param maxValue the maximum possible value that the value being read can be; a lower `maxValue` results
     * in a better compression ratio.
     * @return the value read from this [BitBuffer] as a `long`.
     * @throws IllegalArgumentException if `maxValue` is negative.
     */
    fun getValue(maxValue: Long): Long {
        require(maxValue >= 0) { "maxValue must be positive!" }
        val numBits = Long.SIZE_BITS - maxValue.countLeadingZeroBits() + 1
        val unused = Long.SIZE_BITS - numBits
        return getBits(numBits) shl unused shr unused
    }

    /**
     * Get the capacity of the backing [ByteBuffer].
     *
     * @return the capacity of the backing buffer in `byte`s.
     */
    val capacity: Int
        get() = buffer.capacity()

    var position: Int
        /**
         * Get the position of the backing [ByteBuffer].
         *
         * @return the position of the backing buffer in `byte`s.
         */
        get() = buffer.position()
        /**
         * Sets the position of the backing [ByteBuffer].
         */
        set(value) {
            flushCache()
            buffer.position(value)
        }

    /**
     * Get the remaining of the backing [ByteBuffer].
     *
     * @return the remaining of the backing buffer in `byte`s.
     */
    val remaining: Int
        get() = buffer.remaining() - 7 /* Limit */ - (Long.SIZE_BITS - remainingBits + 7) / Byte.SIZE_BITS /* Cache */

    /**
     * Check if backing [ByteBuffer] has remaining.
     *
     * @return true if it is possible to write on backing [ByteBuffer]
     */
    val hasRemaining: Boolean
        get() = remaining != 0

    /**
     * Compacts the backing [ByteBuffer].
     */
    fun compact() {
        buffer.compact()
    }

    /**
     * Get the backing [ByteBuffer] of this [BitBuffer].
     * <br></br><br></br>
     * Modifying this [ByteBuffer] in any way **will** de-synchronize it from the [BitBuffer]
     * that encompasses it.
     *
     * @return A [ByteBuffer].
     */
    fun toByteBuffer(): ByteBuffer {
        buffer.putLong(cache).rewind()
        buffer.limit(buffer.capacity() - Long.SIZE_BYTES)
        return buffer
    }

}