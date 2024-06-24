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
package io.github.thibaultbee.streampack.core.internal.data

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.internal.utils.extensions.extra
import io.github.thibaultbee.streampack.core.internal.utils.extensions.isAudio
import io.github.thibaultbee.streampack.core.internal.utils.extensions.isVideo
import io.github.thibaultbee.streampack.core.internal.utils.extensions.removePrefixes
import java.nio.ByteBuffer

/**
 * Frame internal representation.
 */
data class Frame(
    /**
     * Contains an audio or video frame data.
     */
    val rawBuffer: ByteBuffer,

    /**
     * Presentation timestamp in µs
     */
    var pts: Long,

    /**
     * Decoded timestamp in µs (not used).
     */
    var dts: Long? = null,

    /**
     * [Boolean.true] if frame is a key frame (I-frame for AVC/HEVC and audio frames)
     */
    val isKeyFrame: Boolean = false,

    /**
     * Contains frame format..
     */
    val format: MediaFormat
) {
    /**
     * Frame mime type
     */
    val mimeType = format.getString(MediaFormat.KEY_MIME)!!

    /**
     * [Boolean.true] if frame is a video frame.
     */
    val isVideo: Boolean = mimeType.isVideo

    /**
     * [Boolean.true] if frame is an audio frame.
     */
    val isAudio: Boolean = mimeType.isAudio

    /**
     * Contains csd buffers for key frames and audio frames only.
     * Could be (SPS, PPS, VPS, etc.) for key video frames, null for non-key video frames.
     * ESDS for AAC frames,...
     */
    val extra = try {
        if (isKeyFrame || mimeType.isAudio) {
            format.extra
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Returns a buffer without prefix csd buffers.
     */
    val buffer = if (extra != null) {
        rawBuffer.removePrefixes(extra)
    } else {
        rawBuffer
    }
}