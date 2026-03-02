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
import io.github.thibaultbee.streampack.core.elements.data.Extra
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.WithClosable
import java.nio.ByteBuffer


/**
 * A pool of [MutableFrame].
 */
internal class FramePool : IClearableObjectPool<Frame> {
    private val pool = ObjectPoolImpl<MutableFrame>()

    fun get(
        rawBuffer: ByteBuffer,
        ptsInUs: Long,
        dtsInUs: Long?,
        isKeyFrame: Boolean,
        extra: Extra?,
        format: MediaFormat,
        onClosed: (Frame) -> Unit = {}
    ): Frame {
        val frame = pool.get()

        val onClosedHook = { frame: MutableFrame ->
            onClosed(frame)
            pool.put(frame)
        }

        return if (frame != null) {
            frame.rawBuffer = rawBuffer
            frame.ptsInUs = ptsInUs
            frame.dtsInUs = dtsInUs
            frame.isKeyFrame = isKeyFrame
            frame.extra = extra
            frame.format = format
            frame.onClosed = onClosedHook
            frame
        } else {
            MutableFrame(
                rawBuffer = rawBuffer,
                ptsInUs = ptsInUs,
                dtsInUs = dtsInUs,
                isKeyFrame = isKeyFrame,
                extra = extra,
                format = format,
                onClosed = onClosedHook
            )
        }
    }

    override fun clear() = pool.clear()

    override fun close() = pool.close()

    /**
     * A mutable [Frame] internal representation.
     *
     * The purpose is to get reusable [Frame]
     */
    private data class MutableFrame(
        /**
         * Contains an audio or video frame data.
         */
        override var rawBuffer: ByteBuffer,

        /**
         * Presentation timestamp in µs
         */
        override var ptsInUs: Long,

        /**
         * Decoded timestamp in µs (not used).
         */
        override var dtsInUs: Long?,

        /**
         * `true` if frame is a key frame (I-frame for AVC/HEVC and audio frames)
         */
        override var isKeyFrame: Boolean,

        /**
         * Contains csd buffers for key frames and audio frames only.
         * Could be (SPS, PPS, VPS, etc.) for key video frames, null for non-key video frames.
         * ESDS for AAC frames,...
         */
        override var extra: Extra?,

        /**
         * Contains frame format..
         * TODO: to remove
         */
        override var format: MediaFormat,

        /**
         * A callback to call when frame is closed.
         */
        override var onClosed: (MutableFrame) -> Unit = {}
    ) : Frame, WithClosable<MutableFrame> {
        override fun close() {
            try {
                onClosed(this)
            } catch (_: Throwable) {
                // Nothing to do
            }
        }
    }

    companion object {
        /**
         * The default frame pool.
         */
        internal val default by lazy { FramePool() }
    }
}