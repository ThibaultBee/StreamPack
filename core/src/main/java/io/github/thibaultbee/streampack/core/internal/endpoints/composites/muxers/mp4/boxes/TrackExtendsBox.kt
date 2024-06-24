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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.boxes

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.models.SampleFlags
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.models.putInt
import java.nio.ByteBuffer

class TrackExtendsBox(
    private val id: Int,
    private val defaultSampleDescriptionIndex: Int = 1,
    private val defaultSampleDuration: Int = 0,
    private val defaultSampleSize: Int = 0,
    private val defaultSampleFlags: SampleFlags = SampleFlags(isNonSyncSample = false)
) : FullBox("trex", 0, 0) {
    override val size: Int = super.size + 20

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.putInt(id)
        output.putInt(defaultSampleDescriptionIndex)
        output.putInt(defaultSampleDuration)
        output.putInt(defaultSampleSize)
        output.putInt(defaultSampleFlags)
    }
}