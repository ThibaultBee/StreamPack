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
package io.github.thibaultbee.streampack.internal.utils

import com.github.thibaultbee.streampack.internal.utils.absoluteValue
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue
import kotlin.math.pow


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

fun ByteBuffer.putShort(l: Long) {
    putShort(l.toShort())
}

fun ByteBuffer.putShort(i: Int) {
    putShort(i.toShort())
}

fun ByteBuffer.putString(s: String) {
    for (c in s.toByteArray(StandardCharsets.UTF_8)) {
        put(c)
    }
}

fun ByteBuffer.indexesOf(prefix: ByteArray): List<Int> {
    if (prefix.isEmpty()) {
        return emptyList()
    }

    val indexes = mutableListOf<Int>()

    outer@ for (i in 0 until this.limit() - prefix.size + 1) {
        for (j in prefix.indices) {
            if (this.get(i + j) != prefix[j]) {
                continue@outer
            }
        }
        indexes.add(i)
    }
    return indexes
}

/**
 * Get all [ByteBuffer] occurrences that start with [prefix].
 *
 */
fun ByteBuffer.slices(prefix: ByteArray): List<ByteBuffer> {
    val slices = mutableListOf<Pair<Int, Int>>()

    // Get all occurrence of prefix in buffer
    val indexes = this.indexesOf(prefix)
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

/**
 * Get max value of a ByteBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return max value that buffer contains
 */
fun ByteBuffer.getMaxValuePerChannel(numOfChannels: Int): List<Byte> {
    val maxValue = MutableList<Byte>(numOfChannels) { 0 }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
        }
    }
    this.rewind()
    return maxValue
}

/**
 * Get max value of a ShortBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return max value that buffer contains
 */
fun ShortBuffer.getMaxValuePerChannel(numOfChannels: Int): List<Short> {
    val maxValue = MutableList<Short>(numOfChannels) { 0 }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
        }
    }
    this.rewind()
    return maxValue
}

/**
 * Get max value of an IntBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return max value that buffer contains
 */
fun IntBuffer.getMaxValuePerChannel(numOfChannels: Int): List<Int> {
    val maxValue = MutableList(numOfChannels) { 0 }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
        }
    }
    this.rewind()
    return maxValue
}

/**
 * Get max value of a FloatBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return max value that buffer contains
 */
fun FloatBuffer.getMaxValuePerChannel(numOfChannels: Int): List<Float> {
    val maxValue = MutableList(numOfChannels) { 0F }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
        }
    }
    this.rewind()
    return maxValue
}

/**
 * Get square sum value of a ByteBuffer
 *
 * * @param numOfChannels number of channel
 * @return square sum value
 */
fun ByteBuffer.getSquareSumPerChannel(numOfChannels: Int): List<Float> {
    val squareSum = MutableList(numOfChannels) { 0F }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return squareSum
}

/**
 * Get square sum value of a ShortBuffer
 *
 * @param numOfChannels number of channel
 * @return square sum value
 */
fun ShortBuffer.getSquareSumPerChannel(numOfChannels: Int): List<Float> {
    val squareSum = MutableList(numOfChannels) { 0F }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return squareSum
}

/**
 * Get square sum value of a IntBuffer
 *
 * @param numOfChannels number of channel
 * @return square sum value
 */
fun IntBuffer.getSquareSumPerChannel(numOfChannels: Int): List<Float> {
    val squareSum = MutableList(numOfChannels) { 0F }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return squareSum
}

/**
 * Get square sum value of a FloatBuffer
 *
 * @param numOfChannels number of channel
 * @return square sum value
 */
fun FloatBuffer.getSquareSumPerChannel(numOfChannels: Int): List<Float> {
    val squareSum = MutableList(numOfChannels) { 0F }
    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            squareSum[it] += this.get().pow(2)
        }
    }
    this.rewind()
    return squareSum
}


/**
 * Get max value and square sum of a ByteBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return a pair containing max value and square root
 */
fun ByteBuffer.getMaxValueAndSquareSumPerChannel(numOfChannels: Int): Pair<List<Byte>, List<Float>> {
    val maxValue = MutableList<Byte>(numOfChannels) { 0 }
    val squareSum = MutableList(numOfChannels) { 0F }

    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return Pair(maxValue, squareSum)
}

/**
 * Get max value and square sum of a ShortBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return a pair containing max value and square root
 */
fun ShortBuffer.getMaxValueAndSquareSumPerChannel(numOfChannels: Int): Pair<List<Short>, List<Float>> {
    val maxValue = MutableList<Short>(numOfChannels) { 0 }
    val squareSum = MutableList(numOfChannels) { 0F }

    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return Pair(maxValue, squareSum)
}

/**
 * Get max value and square sum of an IntBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return a pair containing max value and square root
 */
fun IntBuffer.getMaxValueAndSquareSumPerChannel(numOfChannels: Int): Pair<List<Int>, List<Float>> {
    val maxValue = MutableList(numOfChannels) { 0 }
    val squareSum = MutableList(numOfChannels) { 0F }

    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
            squareSum[it] += this.get().toFloat().pow(2)
        }
    }
    this.rewind()
    return Pair(maxValue, squareSum)
}

/**
 * Get max value and square sum of a FloatBuffer.
 * Audio buffer are order this way: L.R.L.R.L.R
 *
 * @param numOfChannels number of channel
 * @return a pair containing max value and square root
 */
fun FloatBuffer.getMaxValueAndSquareSumPerChannel(numOfChannels: Int): Pair<List<Float>, List<Float>> {
    val maxValue = MutableList(numOfChannels) { 0F }
    val squareSum = MutableList(numOfChannels) { 0F }

    while (hasRemaining()) {
        (0 until numOfChannels).forEach {
            maxValue[it] = this.get().absoluteValue.coerceAtLeast(maxValue[it])
            squareSum[it] += this.get().pow(2)
        }
    }
    this.rewind()
    return Pair(maxValue, squareSum)
}