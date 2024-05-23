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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import java.nio.ByteBuffer

class TimeToSampleBox(
    private val decodingTimes: List<Entry>
) : FullBox("stts", 0, 0) {
    override val size: Int = super.size + 4 + 8 * decodingTimes.size

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(decodingTimes.size)
        decodingTimes.forEach { it.write(output) }
    }

    data class Entry(private val count: Int, val delta: Int) : ByteBufferWriter() {
        override val size: Int = 8

        override fun write(output: ByteBuffer) {
            output.putInt(count)
            output.putInt(delta)
        }
    }

    companion object {
        /**
         * Create a TimeToSampleBox from decoding times.
         *
         * @param dtsList list of decoding times
         * @param hasUnknownLastDelta true if last delta is unknown. It will be set to 0.
         * @return a TimeToSampleBox
         */
        fun fromDts(dtsList: List<Long>, hasUnknownLastDelta: Boolean): TimeToSampleBox {
            if (dtsList.isEmpty()) {
                throw IllegalArgumentException("dtsList must not be empty")
            }
            if (dtsList.size == 1) {
                return TimeToSampleBox(listOf(Entry(1, 0)))
            }

            val compactedEntries = mutableListOf<Entry>()
            var count = 1
            var duration = 0
            for (i in 1 until dtsList.size) {
                val delta = (dtsList[i] - dtsList[i - 1]).toInt()
                if (duration == delta) {
                    count++
                } else {
                    if (i != 1) {
                        compactedEntries.add(Entry(count, duration))
                    }
                    count = 1
                    duration = delta
                }
            }
            compactedEntries.add(Entry(count, duration))

            // Last entry
            if (hasUnknownLastDelta) {
                compactedEntries.add(Entry(1, 0))
            }

            return TimeToSampleBox(compactedEntries)
        }
    }
}