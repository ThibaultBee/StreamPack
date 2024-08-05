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
package io.github.thibaultbee.streampack.core.internal.utils.extensions

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

fun ByteBuffer.put(buffer: ByteBuffer, offset: Int, length: Int) {
    val limit = buffer.limit()
    buffer.position(offset)
    buffer.limit(offset + length)
    this.put(buffer)
    buffer.limit(limit)
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

fun ByteBuffer.indicesOf(prefix: ByteArray): List<Int> {
    if (prefix.isEmpty()) {
        return emptyList()
    }

    val indices = mutableListOf<Int>()

    outer@ for (i in 0 until this.limit() - prefix.size + 1) {
        for (j in prefix.indices) {
            if (this.get(i + j) != prefix[j]) {
                continue@outer
            }
        }
        indices.add(i)
    }
    return indices
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
            indexes[index + 1] - 1
        } else {
            this.limit() - 1
        }
        slices.add(Pair(i, nextPosition))
    }
    val array = this.array()
    return slices.map {
        ByteBuffer.wrap(array.sliceArray(IntRange(it.first, it.second)))
    }
}

/**
 * Whether the [ByteBuffer] starts with a [ByteArray] [prefix] from the current position.
 */
fun ByteBuffer.startsWith(prefix: ByteArray): Boolean {
    if (this.remaining() < prefix.size) {
        return false
    }

    val position = this.position()
    for (i in prefix.indices) {
        if (this.get(position + i) != prefix[i]) {
            return false
        }
    }
    return true
}

/**
 * Whether the [ByteBuffer] starts with a [String] [prefix] from the current position.
 */
fun ByteBuffer.startWith(prefix: String) = startsWith(prefix.toByteArray())


/**
 * Whether the [ByteBuffer] starts with a [ByteBuffer] [prefix] from the current position.
 */
fun ByteBuffer.startWith(prefix: ByteBuffer) = startsWith(prefix.toByteArray())

/**
 * Whether the [ByteBuffer] starts with a list of [ByteBuffer] [prefixes] from the current position.
 *
 * @param prefixes [List] of [ByteBuffer]
 * @return [Pair] of [Boolean] (whether the [ByteBuffer] start with the prefix) and [Int] that is the index of the prefix found (-1 if not found).
 */
fun ByteBuffer.startsWith(prefixes: List<ByteBuffer>): Pair<Boolean, Int> {
    prefixes.forEachIndexed { index, byteBuffer ->
        if (this.startWith(byteBuffer)) {
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
 * Clone [ByteBuffer].
 * The position of the original [ByteBuffer] will be 0 after the clone.
 */
fun ByteBuffer.clone(): ByteBuffer {
    val originalPosition = this.position()
    try {
        val clone = if (isDirect) {
            ByteBuffer.allocateDirect(this.remaining())
        } else {
            ByteBuffer.allocate(this.remaining())
        }
        return clone.put(this).apply { rewind() }
    } finally {
        this.position(originalPosition)
    }
}

/**
 * For AVC and HEVC
 */


/**
 * Get start code size of [ByteBuffer].
 */
val ByteBuffer.startCodeSize: Int
    get() {
        return if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
            && this.get(2) == 0x00.toByte() && this.get(3) == 0x01.toByte()
        ) {
            4
        } else if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
            && this.get(2) == 0x01.toByte()
        ) {
            3
        } else {
            0
        }
    }

fun ByteBuffer.removeStartCode(): ByteBuffer {
    val startCodeSize = this.startCodeSize
    this.position(startCodeSize)
    return this.slice()
}

fun ByteBuffer.extractRbsp(headerLength: Int): ByteBuffer {
    val rbsp = ByteBuffer.allocateDirect(this.remaining())

    val indices = this.indicesOf(byteArrayOf(0x00, 0x00, 0x03))

    rbsp.put(this, this.startCodeSize, headerLength)

    var previous = this.position()
    indices.forEach {
        rbsp.put(this, previous, it + 2 - previous)
        previous = it + 3 // skip emulation_prevention_three_byte
    }
    rbsp.put(this, previous, this.limit() - previous)

    rbsp.limit(rbsp.position())
    rbsp.rewind()
    return rbsp
}

/**
 * Remove all [prefixes] from [ByteBuffer] whatever their order.
 * It slices [ByteBuffer] so it does not copy data.
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

    return this.slice()
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