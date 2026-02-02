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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import io.github.thibaultbee.streampack.core.elements.utils.pool.IBufferPool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

fun ByteBuffer.put(i: Int, i1: Int) {
    put(i, i1.toByte())
}

fun ByteBuffer.put(s: Short) {
    put(s.toByte())
}

fun ByteBuffer.put(i: Int) {
    put(i.toByte())
}

fun ByteBuffer.putInt24(i: Int) {
    putShort(i shr 8)
    put(i.toByte())
}

fun ByteBuffer.putLong48(i: Long) {
    putShort(i shr 32)
    putInt(i.toInt())
}

fun ByteBuffer.putInt(l: Long) {
    putInt(l.toInt())
}

fun ByteBuffer.putInt(d: Double) {
    putInt(d.toInt())
}

fun ByteBuffer.putShort(l: Long) {
    putShort(l.toShort())
}

fun ByteBuffer.putShort(i: Int) {
    putShort(i.toShort())
}

fun ByteBuffer.putShort(f: Float) {
    putShort(f.toInt().toShort())
}

fun ByteBuffer.putShort(d: Double) {
    putShort(d.toInt().toShort())
}

fun ByteBuffer.putString(s: String) {
    put(s.toByteArray())
}

fun ByteBuffer.putFixed88(f: Float) {
    putShort(f * 256.0)
}

fun ByteBuffer.putFixed1616(f: Float) {
    putInt(f * 65536.0)
}

fun ByteBuffer.putFixed1616(i: Int) {
    putInt(i * 65536.0)
}

fun ByteBuffer.put3x3Matrix(matrix: IntArray) {
    require(matrix.size == 9) { "transformationMatrix must be a 9-element array" }
    matrix.forEach { putInt(it) }
}

fun ByteBuffer.put(src: ByteBuffer, offset: Int, length: Int) {
    val limit = src.limit()
    if (offset != 0) {
        src.position(src.position() + offset)
    }
    src.limit(src.position() + offset + length)
    this.put(src)
    src.limit(limit)
}

fun ByteBuffer.getString(size: Int = this.remaining()): String {
    val bytes = ByteArray(size)
    this.get(bytes)
    return String(bytes, StandardCharsets.UTF_8)
}

fun ByteBuffer.getLong(isLittleEndian: Boolean): Long {
    if (isLittleEndian) {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    val value = long
    if (isLittleEndian) {
        order(ByteOrder.BIG_ENDIAN)
    }
    return value
}

/**
 * Finds all occurrences of the given [needle] byte array within the ByteBuffer.
 * @param needle The byte array sequence to search for.
 * @return A list of starting indices for every match found.
 */
fun ByteBuffer.indicesOf(needle: ByteArray): List<Int> {
    if (needle.isEmpty()) return emptyList()

    val results = mutableListOf<Int>()
    val end = limit() - needle.size
    var i = position()

    while (i <= end) {
        if (match(i, needle)) {
            results.add(i)
            // Move forward by the needle's length to find the next non-overlapping match
            // Or use i++ if you want to allow overlapping matches (e.g., "AAA" in "AAAA")
            i += needle.size
        } else {
            i++
        }
    }
    return results
}

private fun ByteBuffer.match(start: Int, needle: ByteArray): Boolean {
    for (idx in needle.indices) {
        if (get(start + idx) != needle[idx]) {
            return false
        }
    }
    return true
}

/**
 * Get all [ByteBuffer] occurrences that start with [prefix].
 */
fun ByteBuffer.slices(prefix: ByteArray): List<ByteBuffer> {
    val slices = mutableListOf<Pair<Int, Int>>()

    // Get all occurrence of prefix in buffer
    val indexes = this.indicesOf(prefix)

    // Get slices
    indexes.forEachIndexed { index, i ->
        val nextPosition = if (indexes.indices.contains(index + 1)) {
            indexes[index + 1]
        } else {
            this.limit()
        }
        slices.add(Pair(i, nextPosition))
    }
    return slices.map {
        this.slice(from = it.first, to = it.second)
    }
}

/**
 * Whether the [ByteBuffer] starts with a [ByteArray] [prefix] from the current position.
 */
fun ByteBuffer.startsWith(
    prefix: ByteArray,
    prefixSkip: Int = 0
): Boolean {
    val size = minOf(remaining(), prefix.size - prefixSkip)
    if (size <= 0) return false

    val position = position()

    for (i in 0 until size) {
        if (get(position + i) != prefix[prefixSkip + i]) return false
    }

    return true
}


/**
 * Whether the [ByteBuffer] starts with a [ByteArray] [prefix] from the current position.
 */
fun ByteBuffer.startsWith(
    prefix: ByteBuffer,
    prefixSkip: Int = 0
): Boolean {
    val size = minOf(remaining(), prefix.remaining() - prefixSkip)
    if (size <= 0) return false

    val position = position()
    val prefixPosition = prefix.position() + prefixSkip

    for (i in 0 until size) {
        if (get(position + i) != prefix.get(prefixPosition + i)) return false
    }

    return true
}

/**
 * Whether the [ByteBuffer] starts with a [String] [prefix] from the current position.
 */
fun ByteBuffer.startsWith(prefix: String) = startsWith(prefix.toByteArray())

/**
 * Whether the [ByteBuffer] starts with a list of [ByteBuffer] [prefixes] from the current position.
 *
 * @param prefixes [List] of [ByteBuffer]
 * @return [Pair] of [Boolean] (whether the [ByteBuffer] start with the prefix) and [Int] that is the index of the prefix found (-1 if not found).
 */
fun ByteBuffer.startsWith(prefixes: List<ByteBuffer>): Pair<Boolean, Int> {
    prefixes.forEachIndexed { index, byteBuffer ->
        if (this.startsWith(byteBuffer)) {
            return Pair(true, index)
        }
    }
    return Pair(false, -1)
}


/**
 * Returns ByteBuffer array even if [ByteBuffer.hasArray] returns false.
 *
 * @return [ByteArray] extracted from [ByteBuffer]
 */
fun ByteBuffer.toByteArray(): ByteArray {
    return if (this.hasArray() && !isDirect) {
        val offset = position() + arrayOffset()
        val array = array()
        if (offset == 0 && array.size == remaining()) {
            array
        } else {
            array.copyOfRange(offset, offset + remaining())
        }
    } else {
        val byteArray = ByteArray(this.remaining())
        get(byteArray)
        byteArray
    }
}

/**
 * Deep copy of [ByteBuffer].
 * The position of the original [ByteBuffer] will be 0 after the clone.
 */
@Deprecated("Use ByteBufferPool instead")
fun ByteBuffer.deepCopy(): ByteBuffer {
    val originalPosition = this.position()
    try {
        val copy = if (isDirect) {
            ByteBuffer.allocateDirect(this.remaining())
        } else {
            ByteBuffer.allocate(this.remaining())
        }
        return copy.put(this).apply { rewind() }
    } finally {
        this.position(originalPosition)
    }
}

/**
 * Deep copy of [ByteBuffer] from [IBufferPool].
 *
 * Don't forget to put the returned [ByteBuffer] to the buffer pool when you are done with it.
 *
 * @param pool [IBufferPool] to use
 * @return [ByteBuffer] deep copy
 */
fun ByteBuffer.deepCopy(pool: IBufferPool<ByteBuffer>): ByteBuffer {
    val originalPosition = this.position()
    try {
        val copy = pool.get(this.remaining())
        return copy.put(this).apply { rewind() }
    } finally {
        this.position(originalPosition)
    }
}

/**
 * For AVC and HEVC
 */


/**
 * Get start code size of [ByteBuffer] from the current position
 */
val ByteBuffer.startCodeSize: Int
    get() = getStartCodeSize(this.position())

/**
 * Get start code size of [ByteBuffer] from the given [position].
 */
fun ByteBuffer.getStartCodeSize(
    position: Int
): Int {
    return if (remaining() >= 4 && this.get(position) == 0x00.toByte() && this.get(position + 1) == 0x00.toByte()
        && this.get(position + 2) == 0x00.toByte() && this.get(position + 3) == 0x01.toByte()
    ) {
        4
    } else if (remaining() >= 3 && this.get(position) == 0x00.toByte() && this.get(position + 1) == 0x00.toByte()
        && this.get(position + 2) == 0x01.toByte()
    ) {
        3
    } else {
        0
    }
}

/**
 * Moves the position after the start code.
 */
fun ByteBuffer.skipStartCode(): ByteBuffer {
    val startCodeSize = this.startCodeSize
    if (startCodeSize > 0) {
        this.position(this.position() + startCodeSize)
    }
    return this
}

private val emulationPreventionThreeByte = byteArrayOf(0x00, 0x00, 0x03)

/**
 * Removes all emulation prevention three bytes from [ByteBuffer].
 *
 * @param headerLength [Int] of the header length before writing the [ByteBuffer].
 * @return [ByteBuffer] without emulation prevention three bytes
 */
fun ByteBuffer.extractRbsp(headerLength: Int): ByteBuffer {
    val indices = this.indicesOf(emulationPreventionThreeByte)

    val rbspSize =
        this.remaining() - indices.size // remove 0x3 bytes for each emulation prevention three bytes
    val rbsp = ByteBuffer.allocateDirect(rbspSize)

    // Write header to new buffer
    rbsp.put(this, 0, headerLength + this.startCodeSize)

    indices.forEach {
        rbsp.put(this, 0, it + 2 - this.position())
        this.position(this.position() + 1) // skip emulation_prevention_three_byte
    }
    rbsp.put(this, 0, this.limit() - this.position())

    rbsp.rewind()
    return rbsp
}

/**
 * Remove all [prefixes] from [ByteBuffer] whatever their order.
 * It moves the [position] of the [ByteBuffer].
 *
 * Once a prefix is found, it is removed from the [prefixes] list.
 *
 * @param prefixes [List] of [ByteBuffer] to remove
 * @return [ByteBuffer] without prefixes
 */
fun ByteBuffer.removePrefixes(prefixes: List<ByteBuffer>): ByteBuffer {
    if (prefixes.isEmpty()) {
        return this
    }
    val mutablePrefixes = prefixes.toMutableList()
    var hasPrefix = true
    while (hasPrefix) {
        val result = this.startsWith(mutablePrefixes)
        hasPrefix = result.first
        if (hasPrefix) {
            this.position(this.position() + mutablePrefixes[result.second].limit())
            mutablePrefixes.removeAt(result.second)
        }
    }

    return this
}

/**
 * Whether [ByteBuffer] is Annex B or not.
 * Annex B frames start with a start code (0x00000001 or 0x000001).
 */
val ByteBuffer.isAnnexB: Boolean
    get() = this.startCodeSize != 0


/**
 * Whether [ByteBuffer] is AVCC/HVCC or not.
 * AVCC/HVCC frames start with a the frame size.
 */
val ByteBuffer.isAvcc: Boolean
    get() {
        val size = this.getInt(0)
        return size == (this.remaining() - 4)
    }

/**
 * Slices the buffer from [from] position to [to] position.
 *
 * @param from start position
 * @param to end position
 */
fun ByteBuffer.slice(from: Int, to: Int): ByteBuffer {
    val currentPosition = this.position()
    val currentLimit = this.limit()
    this.position(from)
    this.limit(to)
    val newBuffer = this.slice()
    this.position(currentPosition)
    this.limit(currentLimit)
    return newBuffer
}
