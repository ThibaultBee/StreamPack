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
import io.github.thibaultbee.streampack.core.elements.utils.extensions.deepCopy
import io.github.thibaultbee.streampack.core.elements.utils.pool.FramePool
import io.github.thibaultbee.streampack.core.elements.utils.pool.IBufferPool
import io.github.thibaultbee.streampack.core.elements.utils.pool.RawFramePool
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * Encoded frame representation
 */
interface RawFrame : Closeable {
    /**
     * Contains an audio or video frame data.
     */
    val rawBuffer: ByteBuffer

    /**
     * Presentation timestamp in µs
     */
    val timestampInUs: Long
}

/**
 * Deep copy the [RawFrame.rawBuffer] into a new [RawFrame].
 *
 * For better memory allocation, you should close the returned frame after usage.
 */
fun RawFrame.deepCopy(
    bufferPool: IBufferPool<ByteBuffer>,
    timestampInUs: Long = this.timestampInUs,
    onClosed: (RawFrame) -> Unit = {}
): RawFrame {
    val copy = this.rawBuffer.deepCopy(bufferPool)
    return copy(
        rawBuffer = copy, timestampInUs = timestampInUs, onClosed = {
            onClosed(it)
            bufferPool.put(copy)
        }
    )
}

/**
 * Copy a [RawFrame] to a new [RawFrame].
 *
 * For better memory allocation, you should close the returned frame after usage.
 */
fun RawFrame.copy(
    rawBuffer: ByteBuffer = this.rawBuffer,
    timestampInUs: Long = this.timestampInUs,
    onClosed: (RawFrame) -> Unit = {}
): RawFrame {
    val pool = RawFramePool.default
    return pool.get(
        rawBuffer, timestampInUs,
        { frame ->
            onClosed(frame)
        })
}

/**
 * A mutable [RawFrame] internal representation.
 *
 * The purpose is to get reusable [RawFrame]
 */
data class MutableRawFrame(
    /**
     * Contains an audio or video frame data.
     */
    override var rawBuffer: ByteBuffer,

    /**
     * Presentation timestamp in µs
     */
    override var timestampInUs: Long,
    /**
     * A callback to call when frame is closed.
     */
    override var onClosed: (MutableRawFrame) -> Unit = {}
) : RawFrame, WithClosable<MutableRawFrame> {
    override fun close() {
        try {
            onClosed(this)
        } catch (_: Throwable) {
            // Nothing to do
        }
    }
}

/**
 * Encoded frame representation
 */
interface Frame : Closeable {
    /**
     * Contains an audio or video frame data.
     */
    val rawBuffer: ByteBuffer

    /**
     * Presentation timestamp in µs
     */
    val ptsInUs: Long

    /**
     * Decoded timestamp in µs (not used).
     */
    val dtsInUs: Long?

    /**
     * `true` if frame is a key frame (I-frame for AVC/HEVC and audio frames)
     */
    val isKeyFrame: Boolean

    /**
     * Gets the csd buffers for key frames and audio frames.
     *
     * Could be (SPS, PPS, VPS, etc.) for key video frames, null for non-key video frames.
     * ESDS for AAC frames,...
     */
    val extra: Extra?

    /**
     * Contains frame format..
     * TODO: to remove
     */
    val format: MediaFormat
}

interface WithClosable<T> {
    val onClosed: (T) -> Unit
}

/**
 * Copy a [Frame] to a new [Frame].
 *
 * For better memory allocation, you should close the returned frame after usage.
 */
fun Frame.copy(
    rawBuffer: ByteBuffer = this.rawBuffer,
    ptsInUs: Long = this.ptsInUs,
    dtsInUs: Long? = this.dtsInUs,
    isKeyFrame: Boolean = this.isKeyFrame,
    extra: Extra? = this.extra,
    format: MediaFormat = this.format,
    onClosed: (Frame) -> Unit = {}
): Frame {
    val pool = FramePool.default
    return pool.get(
        rawBuffer, ptsInUs, dtsInUs, isKeyFrame, extra, format,
        { frame ->
            onClosed(frame)
        })
}


/**
 * A mutable [Frame] internal representation.
 *
 * The purpose is to get reusable [Frame]
 */
data class MutableFrame(
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

/**
 * Ensures that extra are not used at the same time.
 *
 * After accessing the extra, they are automatically rewind for a new usage.
 */
class Extra(private val extraBuffers: List<ByteBuffer>) {
    private val lock = Any()

    val _length by lazy { extraBuffers.sumOf { it.remaining() } }

    fun getLength(): Int {
        return synchronized(lock) {
            _length
        }
    }

    fun <T> get(extra: List<ByteBuffer>.() -> T): T {
        return synchronized(lock) {
            val result = extraBuffers.extra()
            extraBuffers.forEach { it.rewind() }
            result
        }
    }
}

/**
 * Gets the duplicated extra.
 *
 * Prefers to use [Extra.get] as it does not create new resources.
 */
val Extra.extra: List<ByteBuffer>
    get() = get { this.map { it.duplicate() } }

/**
 * Gets the duplicated extra at [index]
 *
 * Prefers to use [Extra.get] as it does not create new resources.
 */
fun Extra.get(index: Int): ByteBuffer {
    return get { this[index].duplicate() }
}
