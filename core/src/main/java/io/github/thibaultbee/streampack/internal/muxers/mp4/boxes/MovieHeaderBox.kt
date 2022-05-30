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

import io.github.thibaultbee.streampack.internal.muxers.mp4.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.put3x3Matrix
import io.github.thibaultbee.streampack.internal.utils.putFixed1616
import io.github.thibaultbee.streampack.internal.utils.putFixed88
import io.github.thibaultbee.streampack.internal.utils.putInt
import java.nio.ByteBuffer

class MovieHeaderBox(
    version: Byte,
    private val creationTime: Long = TimeUtils.utcNowIsom,
    private val modificationTime: Long = TimeUtils.utcNowIsom,
    private val timescale: Int,
    private val duration: Long,
    private val rate: Float = 1.0f,
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
    private val nextTrackId: Int
) : FullBox("mvhd", version, 0) {
    init {
        require(nextTrackId > 0) { "nextTrackId must be greater than 0" }
    }

    override val size: Int = super.size + if (version == 1.toByte()) {
        28
    } else {
        16
    } + 80

    override fun write(buffer: ByteBuffer) {
        super.write(buffer)
        when (version) {
            1.toByte() -> {
                buffer.putLong(creationTime)
                buffer.putLong(modificationTime)
                buffer.putInt(timescale)
                buffer.putLong(duration)
            }
            0.toByte() -> {
                buffer.putInt(creationTime)
                buffer.putInt(modificationTime)
                buffer.putInt(timescale)
                buffer.putInt(duration)
            }
            else -> throw IllegalArgumentException("version must be 0 or 1")
        }
        buffer.putFixed1616(rate)
        buffer.putFixed88(volume)
        buffer.put(ByteArray(10)) // reserved
        buffer.put3x3Matrix(transformationMatrix)
        buffer.put(ByteArray(24)) // pre_defined
        buffer.putInt(nextTrackId)
    }
}