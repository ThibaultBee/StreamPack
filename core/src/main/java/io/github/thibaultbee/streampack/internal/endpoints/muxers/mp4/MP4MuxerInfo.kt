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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4

import android.media.MediaFormat
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxerInfo

object MP4MuxerInfo : IMuxerInfo {
    override val audio = AudioMP4MuxerInfo
    override val video = VideoMP4MuxerInfo
}

object AudioMP4MuxerInfo : IMuxerInfo.IAudioMuxerInfo {
    /**
     * Get MP4 Muxer supported audio encoders list
     */
    override val supportedEncoders =
        listOf(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            MediaFormat.MIMETYPE_AUDIO_OPUS
        )

    override val supportedSampleRates: List<Int>? = null

    override val supportedByteFormats: List<Int>? = null
}

object VideoMP4MuxerInfo :
    IMuxerInfo.IVideoMuxerInfo {
    /**
     * Get MP4 Muxer supported video encoders list
     */
    override val supportedEncoders =
        listOf(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            MediaFormat.MIMETYPE_VIDEO_VP9,
            MediaFormat.MIMETYPE_VIDEO_AV1
        )
}
