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
package com.github.thibaultbee.streampack.data

import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.util.Size
import com.github.thibaultbee.streampack.streamers.BaseCaptureStreamer
import com.github.thibaultbee.streampack.utils.isVideo

/**
 * Video configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [BaseCaptureStreamer.configure]
 */
data class VideoConfig(
    /**
     * Video encoder mime type.
     * Only [MediaFormat.MIMETYPE_VIDEO_AVC] is supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String,
    /**
     * Video encoder bitrate in bits/s.
     */
    val startBitrate: Int,
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size,
    /**
     * Video framerate.
     * This is a best effort as few camera can not generate a fixed framerate.
     */
    val fps: Int,
    /**
     * Video encoder profile.
     */
    val profile: Int = CodecProfileLevel.AVCProfileHigh,
    /**
     * Video encoder level.
     */
    val level: Int = CodecProfileLevel.AVCLevel52,
) {
    init {
        require(mimeType.isVideo()) { "Mime Type must be video"}
    }
}

