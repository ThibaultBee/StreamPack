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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import java.nio.ByteBuffer

class SampleToChunkBox(private val chunkEntries: List<Entry>) : FullBox("stsc", 0, 0) {
    override val size: Int = super.size + 4 + 12 * chunkEntries.size

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(chunkEntries.size)
        chunkEntries.forEach { it.write(output) }
    }

    class Entry(
        private val firstChunk: Int,
        val samplesPerChunk: Int,
        private val sampleDescriptionId: Int,
    ) : ByteBufferWriter() {
        override val size: Int = 12

        override fun write(output: ByteBuffer) {
            output.putInt(firstChunk)
            output.putInt(samplesPerChunk)
            output.putInt(sampleDescriptionId)
        }
    }
}