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
package io.github.thibaultbee.streampack.internal.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.internal.muxers.mp4.models.putInt
import java.nio.ByteBuffer

class TrackRunBox(
    version: Byte,
    private val dataOffset: Int? = null,
    private val firstSampleFlags: SampleFlags? = null,
    private val entries: List<Entry> = emptyList()
) :
    FullBox("trun", version, createFlags(dataOffset, firstSampleFlags, entries)) {
    init {
        require(entries.all { it.duration != null } or entries.all { it.duration == null })
        require(entries.all { it.size != null } or entries.all { it.size == null })
        require(entries.all { it.flags != null } or entries.all { it.flags == null })
        require(entries.all { it.compositionTimeOffset != null } or entries.all { it.compositionTimeOffset == null })
    }

    override val size: Int =
        super.size + 4 + (dataOffset?.let { 4 } ?: 0) + (firstSampleFlags?.let { 4 }
            ?: 0) + entries.sumOf {
            (it.duration?.let { 4 } ?: 0) + (it.size?.let { 4 } ?: 0) + (it.flags?.let { 4 }
                ?: 0) + (it.compositionTimeOffset?.let { 4 } ?: 0)
        }

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.putInt(entries.size)
        dataOffset?.let { buffer.putInt(it) }
        firstSampleFlags?.let { buffer.putInt(it) }
        entries.forEach {
            buffer.put(it)
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
            entries[0].duration?.let { flags += TrackRunFlag.SAMPLE_DURATION_PRESENT.value }
            entries[0].size?.let { flags += TrackRunFlag.SAMPLE_SIZE_PRESENT.value }
            entries[0].flags?.let { flags += TrackRunFlag.SAMPLE_FLAGS_PRESENT.value }
            entries[0].compositionTimeOffset?.let { flags += TrackRunFlag.SAMPLE_COMPOSITION_TIME_OFFSETS_PRESENT.value }
            return flags
        }
    }

    class Entry(
        val duration: Int? = null,
        val size: Int? = null,
        val flags: Int? = null,
        val compositionTimeOffset: Int? = null
    )

    private fun ByteBuffer.put(e: Entry) {
        e.duration?.let { this.putInt(it) }
        e.size?.let { this.putInt(it) }
        e.flags?.let { this.putInt(it) }
        e.compositionTimeOffset?.let { this.putInt(it) }
    }
}