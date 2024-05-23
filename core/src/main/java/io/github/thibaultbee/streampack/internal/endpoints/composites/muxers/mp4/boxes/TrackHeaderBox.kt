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

import android.util.Size
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.put3x3Matrix
import io.github.thibaultbee.streampack.internal.utils.extensions.putFixed1616
import io.github.thibaultbee.streampack.internal.utils.extensions.putFixed88
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt
import java.nio.ByteBuffer

class TrackHeaderBox(
    version: Byte,
    flags: List<TrackFlag>,
    val id: Int,
    private val creationTime: Long = TimeUtils.utcNowIsom,
    private val modificationTime: Long = TimeUtils.utcNowIsom,
    private val duration: Long,
    private val layer: Short = 0,
    private val alternateGroup: Short = 0,
    private val volume: Float = 1.0f,
    private val transformationMatrix: IntArray = intArrayOf(
        0x10000,
        0,
        0,
        0,
        0x10000,
        0,
        0,
        0,
        0x40000000
    ),
    private val resolution: Size
) :
    FullBox("tkhd", version, flags.toFlags()) {

    init {
        require(id > 0) { "trackId must be greater than 0" }
    }

    override val size: Int
        get() = super.size + if (version == 1.toByte()) {
            32
        } else {
            20
        } + 60

    override fun write(output: ByteBuffer) {
        super.write(output)
        when (version) {
            1.toByte() -> {
                output.putLong(creationTime)
                output.putLong(modificationTime)
                output.putInt(id)
                output.putInt(0) // reserved
                output.putLong(duration)
            }
            0.toByte() -> {
                output.putInt(creationTime)
                output.putInt(modificationTime)
                output.putInt(id)
                output.putInt(0) // reserved
                output.putInt(duration)
            }
            else -> throw IllegalArgumentException("version must be 0 or 1")
        }
        output.put(ByteArray(8))
        output.putShort(layer) // layer
        output.putShort(alternateGroup) // alternate_group
        output.putFixed88(volume)
        output.putShort(0) // reserved
        output.put3x3Matrix(transformationMatrix)
        output.putFixed1616(resolution.width.toFloat())
        output.putFixed1616(resolution.height.toFloat())
    }

    enum class TrackFlag(val value: Int) {
        ENABLED(0x000001),
        IN_MOVIE(0x000002),
        IN_PREVIEW(0x000004),
        IS_ASPECT_RATIO(0x000008)
    }
}

private fun List<TrackHeaderBox.TrackFlag>.toFlags(): Int {
    var flags = 0
    for (flag in this) {
        flags = flags or flag.value
    }
    return flags
}
