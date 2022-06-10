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

import java.nio.ByteBuffer

class TimeToSampleBox(
    private val decodingTimes: List<Entry>
) : FullBox("stts", 0, 0) {
    override val size: Int = super.size + 4 + 8 * decodingTimes.size

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.putInt(decodingTimes.size)
        decodingTimes.forEach { buffer.put(it) }
    }

    data class Entry(val count: Int, val delta: Int)

    fun ByteBuffer.put(d: Entry) {
        putInt(d.count)
        putInt(d.delta)
    }
}