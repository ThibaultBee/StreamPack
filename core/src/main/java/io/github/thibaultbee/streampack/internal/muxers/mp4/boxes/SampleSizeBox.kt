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

class SampleSizeBox(
    private val sampleSize: Int = 0,
    private val sampleSizeEntries: List<Int>? = null
) : FullBox("stsz", 0, 0) {
    init {
        if (sampleSize == 0) {
            require(sampleSizeEntries != null) { "sampleSizeEntries must be set if sampleSize is 0" }
        }
    }

    override val size: Int = super.size + 8 + (sampleSizeEntries?.size ?: 0) * 4

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        buffer.putInt(sampleSize)
        buffer.putInt(sampleSizeEntries?.size ?: 0)
        sampleSizeEntries?.forEach { buffer.putInt(it) }
    }
}