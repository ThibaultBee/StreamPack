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
package io.github.thibaultbee.streampack.internal.muxers.mp4

import android.media.MediaFormat
import io.github.thibaultbee.streampack.internal.muxers.IAudioMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.IMuxerHelper
import io.github.thibaultbee.streampack.internal.muxers.IVideoMuxerHelper

class MP4MuxerHelper : IMuxerHelper {
    override val audio = AudioMP4MuxerHelper()
    override val video = VideoMP4MuxerHelper()
}

class AudioMP4MuxerHelper : IAudioMuxerHelper {
    /**
     * Get MP4 Muxer supported audio encoders list
     */
    override val supportedEncoders =
        listOf(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            MediaFormat.MIMETYPE_AUDIO_OPUS
        )

    override fun getSupportedSampleRates(): List<Int>? = null

    override fun getSupportedByteFormats(): List<Int>? = null
}

class VideoMP4MuxerHelper : IVideoMuxerHelper {
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
