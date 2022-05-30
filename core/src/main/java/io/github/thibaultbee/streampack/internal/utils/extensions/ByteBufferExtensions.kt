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
package io.github.thibaultbee.streampack.internal.utils.extensions

import io.github.thibaultbee.streampack.internal.utils.av.video.getStartCodeSize
import java.nio.ByteBuffer

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
 *
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
 * Check if [ByteBuffer] starts with [prefix].
 */
fun ByteBuffer.startsWith(prefix: ByteArray): Boolean {
    for (i in prefix.indices) {
        if (this.get(i) != prefix[i]) {
            return false
        }
    }
    return true
}

/**
 * Returns ByteBuffer array even if [ByteBuffer.hasArray] returns false.
 *
 * @return [ByteArray] extracted from [ByteBuffer]
 */
fun ByteBuffer.extractArray(): ByteArray {
    return if (this.hasArray() && !isDirect) {
        this.array()
    } else {
        val byteArray = ByteArray(this.remaining())
        this.get(byteArray)
        byteArray
    }
}

fun ByteBuffer.extractRbsp(headerLength: Int): ByteBuffer {
    val rbsp = ByteBuffer.allocateDirect(this.remaining())

    val indices = this.indicesOf(byteArrayOf(0x00, 0x00, 0x03))

    rbsp.put(this, this.getStartCodeSize(), headerLength)

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