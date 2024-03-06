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

import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models.putInt
import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import java.nio.ByteBuffer

class TrackRunBox(
    version: Byte,
    private val dataOffset: Int? = null,
    private val firstSampleFlags: SampleFlags? = null,
    private val entries: List<Entry> = emptyList()
) :
    FullBox("trun", version, createFlags(dataOffset, firstSampleFlags, entries)) {
    init {
        require(entries.all { it.sampleDuration != null } or entries.all { it.sampleDuration == null })
        require(entries.all { it.sampleFlags != null } or entries.all { it.sampleFlags == null })
        require(entries.all { it.sampleCompositionTimeOffset != null } or entries.all { it.sampleCompositionTimeOffset == null })
    }

    override val size: Int =
        super.size + 4 + (dataOffset?.let { 4 } ?: 0) + (firstSampleFlags?.let { 4 }
            ?: 0) + entries.sumOf {
            (it.sampleDuration?.let { 4 } ?: 0) + it.size.let { 4 } + (it.sampleFlags?.let { 4 }
                ?: 0) + (it.sampleCompositionTimeOffset?.let { 4 } ?: 0)
        }

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(entries.size)
        dataOffset?.let { output.putInt(it) }
        firstSampleFlags?.let { output.putInt(it) }
        entries.forEach {
            it.write(output)
        }
    }

    enum class TrackRunFlag(val value: Int) {
        DATA_OFFSET_PRESENT(0x000001),
        FIRST_SAMPLE_FLAGS_PRESENT(0x000004),
        SAMPLE_DURATION_PRESENT(0x000100),
        SAMPLE_SIZE_PRESENT(0x000200),
        SAMPLE_FLAGS_PRESENT(0x000400),
        SAMPLE_COMPOSITION_TIME_OFFSETS_PRESENT(0x000800)
    }

    companion object {
        private fun createFlags(
            dataOffset: Int?,
            firstSampleFlags: SampleFlags?,
            entries: List<Entry>
        ): Int {
            var flags = 0
            dataOffset?.let { flags += TrackRunFlag.DATA_OFFSET_PRESENT.value }
            firstSampleFlags?.let { flags += TrackRunFlag.FIRST_SAMPLE_FLAGS_PRESENT.value }
            entries[0].sampleDuration?.let { flags += TrackRunFlag.SAMPLE_DURATION_PRESENT.value }
            entries[0].sampleSize?.let { flags += TrackRunFlag.SAMPLE_SIZE_PRESENT.value }
            entries[0].sampleFlags?.let { flags += TrackRunFlag.SAMPLE_FLAGS_PRESENT.value }
            entries[0].sampleCompositionTimeOffset?.let { flags += TrackRunFlag.SAMPLE_COMPOSITION_TIME_OFFSETS_PRESENT.value }
            return flags
        }
    }

    class Entry(
        val sampleDuration: Int? = null,
        val sampleSize: Int? = null,
        val sampleFlags: Int? = null,
        val sampleCompositionTimeOffset: Int? = null
    ) : ByteBufferWriter() {
        override val size: Int = (sampleDuration?.let { 4 } ?: 0) + (sampleSize?.let { 4 }
            ?: 0) + (sampleFlags?.let { 4 }
            ?: 0) + (sampleCompositionTimeOffset?.let { 4 } ?: 0)

        override fun write(output: ByteBuffer) {
            sampleDuration?.let { output.putInt(it) }
            sampleSize?.let { output.putInt(it) }
            sampleFlags?.let { output.putInt(it) }
            sampleCompositionTimeOffset?.let { output.putInt(it) }
        }
    }
}