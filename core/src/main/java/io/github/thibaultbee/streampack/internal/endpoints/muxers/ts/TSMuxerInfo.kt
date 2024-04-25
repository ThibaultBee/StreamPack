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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.ts

import android.media.MediaFormat
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IPublicMuxer

object TSMuxerInfo : IPublicMuxer.IMuxerInfo {
    override val audio = AudioTSMuxerInfo
    override val video = VideoTSMuxerInfo
}

object AudioTSMuxerInfo : IPublicMuxer.IMuxerInfo.IAudioMuxerInfo {
    /**
     * Get TS Muxer supported audio encoders list
     */
    override val supportedEncoders =
        listOf(MediaFormat.MIMETYPE_AUDIO_AAC, MediaFormat.MIMETYPE_AUDIO_OPUS)

    override val supportedSampleRates: List<Int>? = null

    override val supportedByteFormats: List<Int>? = null
}

object VideoTSMuxerInfo : IPublicMuxer.IMuxerInfo.IVideoMuxerInfo {
    /**
     * Get TS Muxer supported video encoders list
     */
    override val supportedEncoders =
        listOf(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            MediaFormat.MIMETYPE_VIDEO_HEVC
        )
}
