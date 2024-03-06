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

import java.nio.ByteBuffer

class MovieExtendsBox(
    private val trex: List<TrackExtendsBox>,
) : Box("mvex") {
    override val size: Int = super.size + trex.sumOf { it.size }

    override fun write(output: ByteBuffer) {
        super.write(output)
        trex.forEach { it.write(output) }
    }
}