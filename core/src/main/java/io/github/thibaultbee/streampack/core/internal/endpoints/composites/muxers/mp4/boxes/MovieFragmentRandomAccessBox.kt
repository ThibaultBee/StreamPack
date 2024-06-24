/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes

import java.nio.ByteBuffer

class MovieFragmentRandomAccessBox(
    private val traf: List<TrackFragmentRandomAccessBox>
) :
    Box("mfra") {
    constructor(
        traf: TrackFragmentRandomAccessBox,
    ) : this(listOf(traf))

    private val mfro =
        MovieFragmentRandomAccessOffsetBox(traf.sumOf { it.size } + 24 /* mfra header + mfro size */)

    override val size: Int = super.size + traf.sumOf { it.size } + mfro.size

    override fun write(output: ByteBuffer) {
        super.write(output)
        traf.forEach { it.write(output) }
        mfro.write(output)
    }
}