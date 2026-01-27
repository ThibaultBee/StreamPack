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

import io.github.thibaultbee.streampack.core.elements.data.MutableRawFrame
import java.nio.ByteBuffer


/**
 * A pool of [MutableRawFrame].
 */
internal class RawFramePool() : ObjectPool<MutableRawFrame>() {
    fun get(
        rawBuffer: ByteBuffer,
        timestampInUs: Long,
        onClosed: (MutableRawFrame) -> Unit = {}
    ): MutableRawFrame {
        val frame = get()

        val onClosedHook = { frame: MutableRawFrame ->
            onClosed(frame)
            put(frame)
        }

        return if (frame != null) {
            frame.rawBuffer = rawBuffer
            frame.timestampInUs = timestampInUs
            frame.onClosed = onClosedHook
            frame
        } else {
            MutableRawFrame(
                rawBuffer = rawBuffer,
                timestampInUs = timestampInUs,
                onClosed = onClosedHook
            )
        }
    }

    companion object {
        /**
         * The default frame pool.
         */
        internal val default by lazy { RawFramePool() }
    }
}