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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IPublicMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.SoundFormat
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.SoundRate
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.SoundSize
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.video.CodecID

object FlvMuxerInfo :
    IPublicMuxer.IMuxerInfo {
    override val video = VideoFlvMuxerInfo
    override val audio = AudioFlvMuxerInfo
}

object AudioFlvMuxerInfo :
    IPublicMuxer.IMuxerInfo.IAudioMuxerInfo {
    /**
     * Get FLV Muxer supported audio encoders list
     */
    override val supportedEncoders =
        SoundFormat.entries.mapNotNull {
            try {
                it.toMimeType()
            } catch (e: Exception) {
                null
            }
        }

    override val supportedSampleRates = SoundRate.entries.map { it.toSampleRate() }

    override val supportedByteFormats = SoundSize.entries.map { it.toByteFormat() }
}

object VideoFlvMuxerInfo :
    IPublicMuxer.IMuxerInfo.IVideoMuxerInfo {
    /**
     * Get FLV Muxer supported video encoders list
     */
    override val supportedEncoders = run {
        val extendedSupportedCodec = listOf(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            MediaFormat.MIMETYPE_VIDEO_VP9,
            MediaFormat.MIMETYPE_VIDEO_AV1
        )
        val supportedCodecList = CodecID.entries.mapNotNull {
            try {
                it.toMimeType()
            } catch (e: Exception) {
                null
            }
        }.filter {
            listOf(
                MediaFormat.MIMETYPE_VIDEO_AVC
            ).contains(it)
        }
        supportedCodecList + extendedSupportedCodec
    }
}