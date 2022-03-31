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
package io.github.thibaultbee.streampack.data

import android.content.Context
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import io.github.thibaultbee.streampack.internal.utils.isPortrait
import io.github.thibaultbee.streampack.internal.utils.isVideo
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import java.io.IOException

/**
 * Video configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [BaseStreamer.configure]
 */
class VideoConfig(
    /**
     * Video encoder mime type.
     * Only [MediaFormat.MIMETYPE_VIDEO_AVC] and [MediaFormat.MIMETYPE_VIDEO_HEVC] are supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    /**
     * Video encoder bitrate in bits/s.
     */
    startBitrate: Int = 2000000,
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size = Size(1280, 720),
    /**
     * Video framerate.
     * This is a best effort as few camera can not generate a fixed framerate.
     */
    val fps: Int = 30,
    /**
     * Video encoder profile. Encoders may not support requested profile. In this case, StreamPack fallbacks to default profile.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val profile: Int = getDefaultProfile(mimeType),
    /**
     * Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to default level.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val level: Int = getDefaultLevel(mimeType)
) : Config(mimeType, startBitrate) {
    init {
        require(mimeType.isVideo()) { "MimeType must be video" }
    }

    /**
     * Get resolution according to device orientation
     *
     * @param context activity context
     * @return oriented resolution
     */
    fun getOrientedResolution(context: Context): Size {
        return if (context.isPortrait()) {
            Size(resolution.height, resolution.width)
        } else {
            Size(resolution.width, resolution.height)
        }
    }

    companion object {
        private fun getDefaultProfile(mimeType: String) =
            when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> CodecProfileLevel.AVCProfileHigh
                MediaFormat.MIMETYPE_VIDEO_HEVC -> CodecProfileLevel.HEVCProfileMain
                else -> throw IOException("Not supported mime type: $mimeType")
            }


        private fun getDefaultLevel(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CodecProfileLevel.AVCLevel62
            } else {
                CodecProfileLevel.AVCLevel52
            }
            MediaFormat.MIMETYPE_VIDEO_HEVC -> CodecProfileLevel.HEVCMainTierLevel62
            else -> throw IOException("Not supported mime type: $mimeType")
        }
    }
}

