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

import io.github.thibaultbee.streampack.internal.utils.av.ByteBufferWriter
import java.nio.ByteBuffer

class TrackFragmentRandomAccessBox(private val id: Int, private val entries: List<Entry>) :
    FullBox("tfra", 1, 0) {
    override val size: Int = super.size + 12 + entries.size * 19

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(id)
        /**
         * length_size_of_traf_num + length_size_of_trun_num + length_size_of_sample_num are forced to 0.
         */
        output.putInt(0) // reserved + length_size_of_traf_num + length_size_of_trun_num + length_size_of_sample_num

        output.putInt(entries.size)
        entries.forEach {
            it.write(output)
        }
    }

    data class Entry(
        private val time: Long,
        private val moofOffset: Long,
        private val trafNumber: Byte = 1,
        private val trunNumber: Byte = 1,
        private val sampleNumber: Byte = 1
    ) : ByteBufferWriter() {
        override val size: Int = 19

        override fun write(output: ByteBuffer) {
            output.putLong(time)
            output.putLong(moofOffset)
            output.put(trafNumber)
            output.put(trunNumber)
            output.put(sampleNumber)
        }
    }
}