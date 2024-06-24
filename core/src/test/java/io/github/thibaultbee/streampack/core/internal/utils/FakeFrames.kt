/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.internal.data.Frame
import java.nio.ByteBuffer
import kotlin.random.Random

object FakeFrames {
    fun generate(
        mimeType: String,
        buffer: ByteBuffer = ByteBuffer.wrap(Random.nextBytes(1024)),
        pts: Long = Random.nextLong(),
        dts: Long? = null,
        isKeyFrame: Boolean = false
    ): Frame {
        MockUtils.mockMediaFormatConstructor(
            mimeType, if (isKeyFrame) {
                ByteBuffer.wrap(
                    Random.nextBytes(10)
                )
            } else {
                null
            }
        )
        val format = MediaFormat()
        format.setString(
            MediaFormat.KEY_MIME,
            mimeType
        )
        if (isKeyFrame) {
            format.setByteBuffer(
                "csd-0",
                ByteBuffer.wrap(
                    Random.nextBytes(10)
                )
            )
        }
        return Frame(
            buffer,
            pts,
            dts,
            isKeyFrame,
            format = format
        )
    }
}