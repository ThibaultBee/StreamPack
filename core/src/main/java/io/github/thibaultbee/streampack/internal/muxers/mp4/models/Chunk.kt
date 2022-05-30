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
import io.github.thibaultbee.streampack.internal.utils.av.ByteBufferWriter
import java.nio.ByteBuffer

data class Chunk(val firstChunk: Int, val sampleDescriptionId: Int) : ByteBufferWriter() {
    private val samples = mutableListOf<Frame>()
    override val size: Int
        get() = samples.sumOf { it.buffer.remaining() }
    val samplesPerChunk: Int
        get() = samples.size

    fun add(frame: Frame) {
        samples.add(frame)
    }

    override fun write(output: ByteBuffer) {
        samples.forEach { output.put(it.buffer) }
    }

    fun toSampleToChunk(): SampleToChunk {
        return SampleToChunk(
            firstChunk = firstChunk,
            samplesPerChunk = samplesPerChunk,
            sampleDescriptionId = sampleDescriptionId
        )
    }
}

fun ByteBuffer.putSampleToChunk(c: Chunk) {
    putInt(c.firstChunk)
    putInt(c.samplesPerChunk)
    putInt(c.sampleDescriptionId)
}