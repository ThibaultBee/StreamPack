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

import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.mp4.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt
import java.nio.ByteBuffer

class MediaHeaderBox(
    version: Byte,
    private val creationTime: Long = TimeUtils.utcNowIsom,
    private val modificationTime: Long = TimeUtils.utcNowIsom,
    private val timescale: Int,
    private val duration: Long,
    private val language: Short = 0,
) : FullBox("mdhd", version, 0) {
    override val size: Int = super.size + if (version == 1.toByte()) {
        28
    } else {
        16
    } + 4

    override fun write(output: ByteBuffer) {
        super.write(output)
        when (version) {
            1.toByte() -> {
                output.putLong(creationTime)
                output.putLong(modificationTime)
                output.putInt(timescale)
                output.putLong(duration)
            }
            0.toByte() -> {
                output.putInt(creationTime)
                output.putInt(modificationTime)
                output.putInt(timescale)
                output.putInt(duration)
            }
            else -> {
                throw IllegalArgumentException("Version must be 0 or 1")
            }
        }
        output.putShort(language)
        output.putShort(0) // pre_defined
    }
}