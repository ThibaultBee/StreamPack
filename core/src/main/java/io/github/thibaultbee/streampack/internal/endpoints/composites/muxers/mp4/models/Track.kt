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
package io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.models

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.utils.TimeUtils

class Track(
    val id: Int,
    val config: Config,
    val timescale: Int = TimeUtils.TIME_SCALE,
) {
    val syncSamples = mutableListOf<SyncSample>()
    val firstTimestamp: Long
        get() = syncSamples.first { !it.isFragment }.time

    init {
        require(id != 0) { "id must be greater than 0" }
    }

    class SyncSample(
        val time: Long,
        val isFragment: Boolean,
        val moofOffset: Long
    )
}


