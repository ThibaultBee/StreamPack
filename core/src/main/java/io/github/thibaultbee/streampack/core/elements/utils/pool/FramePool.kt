/*
 * Copyright 2026 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils.pool

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.elements.data.MutableFrame
import java.nio.ByteBuffer


/**
 * A pool of [MutableFrame].
 */
internal class FramePool() : ObjectPool<MutableFrame>() {
    fun get(
        rawBuffer: ByteBuffer,
        ptsInUs: Long,
        dtsInUs: Long?,
        isKeyFrame: Boolean,
        extra: List<ByteBuffer>?,
        format: MediaFormat,
        onClosed: (MutableFrame) -> Unit
    ): MutableFrame {
        val frame = get()

        return if (frame != null) {
            frame.rawBuffer = rawBuffer
            frame.ptsInUs = ptsInUs
            frame.dtsInUs = dtsInUs
            frame.isKeyFrame = isKeyFrame
            frame.extra = extra
            frame.format = format
            frame.onClosed = onClosed
            frame
        } else {
            MutableFrame(
                rawBuffer = rawBuffer,
                ptsInUs = ptsInUs,
                dtsInUs = dtsInUs,
                isKeyFrame = isKeyFrame,
                extra = extra,
                format = format,
                onClosed = onClosed
            )
        }
    }
}