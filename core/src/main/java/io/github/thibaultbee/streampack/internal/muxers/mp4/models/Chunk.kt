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
package io.github.thibaultbee.streampack.internal.muxers.mp4.models

import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.utils.extensions.unzip
import java.nio.ByteBuffer

/**
 * Storage for frames
 */
class Chunk(val id: Int) {
    private val samples = mutableListOf<IndexedFrame>()

    val numOfSamples: Int
        get() = samples.size

    val dataSize: Int
        get() = samples.sumOf { it.frame.buffer.remaining() }

    val firstTimestamp: Long
        get() = samples.minOf { it.frame.pts }

    private val lastTimestamp: Long
        get() = samples.maxOf { it.frame.pts }

    val duration: Long
        get() = lastTimestamp - firstTimestamp

    val onlySyncFrame: Boolean
        get() = samples.all { it.frame.isKeyFrame }

    val syncFrameList: List<Int>
        get() = samples.filter { it.frame.isKeyFrame }.map { it.id }

    val sampleSizes: List<Int>
        get() = samples.map { it.frame.buffer.remaining() }

    val extra: List<List<ByteBuffer>>
        get() = samples.mapNotNull { it.frame.extra }.unzip()

    val sampleDts: List<Long>
        get() = samples.map {
            it.frame.dts ?: it.frame.pts
        }

    fun add(id: Int, frame: Frame) {
        samples.add(IndexedFrame(id, frame))
    }

    fun writeTo(action: (Frame) -> Unit) {
        samples.forEach { action(it.frame) }
    }

    fun write(output: ByteBuffer) {
        samples.forEach { output.put(it.frame.buffer) }
    }

    class IndexedFrame(val id: Int, val frame: Frame)
}