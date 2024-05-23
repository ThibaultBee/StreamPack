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

import java.nio.ByteBuffer

class MovieBox(
    private val mvhd: MovieHeaderBox,
    val trak: List<TrackBox>,
    private val mvex: MovieExtendsBox? = null
) : Box("moov") {
    init {
        require(trak.isNotEmpty()) { "At least one track is required" }
        require(trak.distinctBy { it.tkhd.id }.size == trak.size) { "All tracks must have different trackId" }
    }

    override val size: Int = super.size + mvhd.size + trak.sumOf { it.size } + (mvex?.size ?: 0)

    override fun write(output: ByteBuffer) {
        super.write(output)
        mvhd.write(output)
        trak.forEach { it.write(output) }
        mvex?.write(output)
    }
}