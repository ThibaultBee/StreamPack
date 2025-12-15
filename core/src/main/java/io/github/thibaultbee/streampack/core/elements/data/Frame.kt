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
package io.github.thibaultbee.streampack.core.elements.data

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.elements.utils.extensions.removePrefixes
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * A raw frame internal representation.
 */
data class RawFrame(
    /**
     * Contains an audio or video frame data.
     */
    val rawBuffer: ByteBuffer,

    /**
     * Presentation timestamp in µs
     */
    var timestampInUs: Long,

    /**
     * A callback to call when frame is closed.
     */
    val onClosed: (RawFrame) -> Unit = {}
) : Closeable {
    override fun close() {
        try {
            onClosed(this)
        } catch (_: Throwable) {
            // Nothing to do
        }
    }
}


data class Frame(
    /**
     * Contains an audio or video frame data.
     */
    val rawBuffer: ByteBuffer,

    /**
     * Presentation timestamp in µs
     */
    val ptsInUs: Long,

    /**
     * Decoded timestamp in µs (not used).
     */
    val dtsInUs: Long? = null,

    /**
     * `true` if frame is a key frame (I-frame for AVC/HEVC and audio frames)
     */
    val isKeyFrame: Boolean,

    /**
     * Contains csd buffers for key frames and audio frames only.
     * Could be (SPS, PPS, VPS, etc.) for key video frames, null for non-key video frames.
     * ESDS for AAC frames,...
     */
    val extra: List<ByteBuffer>?,

    /**
     * Contains frame format..
     * TODO: to remove
     */
    val format: MediaFormat
) {
    init {
        removePrefixes()
    }
}

/**
 * Removes the [Frame.extra] prefixes from the [Frame.rawBuffer].
 *
 * With MediaCodec, the encoded frames may contain prefixes like SPS, PPS for H264/H265 key frames.
 * It also modifies the position of the [Frame.rawBuffer] to skip the prefixes.s
 *
 * @return A [ByteBuffer] without prefixes.
 */
fun Frame.removePrefixes(): ByteBuffer {
    return if (extra != null) {
        rawBuffer.removePrefixes(extra)
    } else {
        rawBuffer
    }
}


fun FrameWithCloseable(
    /**
     * Contains an audio or video frame data.
     */
    rawBuffer: ByteBuffer,

    /**
     * Presentation timestamp in µs
     */
    ptsInUs: Long,

    /**
     * Decoded timestamp in µs (not used).
     */
    dtsInUs: Long?,

    /**
     * `true` if frame is a key frame (I-frame for AVC/HEVC and audio frames)
     */
    isKeyFrame: Boolean,

    /**
     * Contains csd buffers for key frames and audio frames only.
     * Could be (SPS, PPS, VPS, etc.) for key video frames, null for non-key video frames.
     * ESDS for AAC frames,...
     */
    extra: List<ByteBuffer>?,

    /**
     * Contains frame format..
     */
    format: MediaFormat,

    /**
     * A callback to call when frame is closed.
     */
    onClosed: (FrameWithCloseable) -> Unit,
) = FrameWithCloseable(
    Frame(
        rawBuffer,
        ptsInUs,
        dtsInUs,
        isKeyFrame,
        extra,
        format
    ),
    onClosed
)

/**
 * Frame internal representation.
 */
data class FrameWithCloseable(
    val frame: Frame,
    val onClosed: (FrameWithCloseable) -> Unit
) : Closeable {
    override fun close() {
        try {
            onClosed(this)
        } catch (_: Throwable) {
            // Nothing to do
        }
    }
}

/**
 * Uses the resource and unwraps the [Frame] to pass it to the given block.
 */
inline fun <T> FrameWithCloseable.useAndUnwrap(block: (Frame) -> T) = use {
    block(frame)
}