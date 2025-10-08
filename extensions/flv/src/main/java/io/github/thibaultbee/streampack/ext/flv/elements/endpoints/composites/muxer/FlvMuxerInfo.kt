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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer

import android.media.MediaFormat
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvUtils
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvUtils.isLegacyAudioMimeType
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvUtils.isLegacyByteFormat
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvUtils.isLegacyVideoMimeType

object FlvMuxerInfo :
    IMuxer.IMuxerInfo {
    override val video by lazy { VideoFlvMuxerInfo }
    override val audio by lazy { AudioFlvMuxerInfo }

    /**
     * Whether the given config is supported by the FLV legacy format.
     *
     * @param config the codec configuration
     * @return true if the config is supported by the FLV legacy format
     */
    fun isLegacyConfig(config: CodecConfig): Boolean {
        return when (config) {
            is VideoCodecConfig -> VideoFlvMuxerInfo.isLegacyConfig(config)
            is AudioCodecConfig -> AudioFlvMuxerInfo.isLegacyConfig(config)
        }
    }
}

object AudioFlvMuxerInfo :
    IMuxer.IMuxerInfo.IAudioMuxerInfo {
    /**
     * Get FLV Muxer supported audio encoders list
     */
    override val supportedEncoders by lazy {
        val supportedCodecs = listOf(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            MediaFormat.MIMETYPE_AUDIO_OPUS
        )
        AudioMediaType.entries.mapNotNull {
            try {
                FlvUtils.mimeTypeFromAudioMediaType(it)
            } catch (_: Throwable) {
                null
            }
        }.filter {
            supportedCodecs.contains(it)
        }
    }

    /**
     * Get FLV Muxer supported audio sample rates list.
     */
    override val supportedSampleRates = null

    override val supportedByteFormats by lazy {
        SoundSize.entries.map {
            FlvUtils.byteFormatFromSoundSize(it)
        }
    }

    /**
     * Whether the given config is supported by the FLV legacy format.
     *
     * @param config the audio codec configuration
     * @return true if the config is supported by the FLV legacy format
     */
    fun isLegacyConfig(config: AudioCodecConfig): Boolean {
        return isLegacyAudioMimeType(config.mimeType) &&
                isLegacyByteFormat(config.byteFormat)
    }
}

object VideoFlvMuxerInfo :
    IMuxer.IMuxerInfo.IVideoMuxerInfo {
    /**
     * Get FLV Muxer supported video encoders list
     */
    override val supportedEncoders by lazy {
        val supportedCodecs = listOf(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            MediaFormat.MIMETYPE_VIDEO_VP9,
            MediaFormat.MIMETYPE_VIDEO_AV1
        )
        VideoMediaType.entries.mapNotNull {
            try {
                FlvUtils.mimeTypeFromVideoMediaType(it)
            } catch (_: Throwable) {
                null
            }
        }.filter {
            supportedCodecs.contains(it)
        }
    }

    /**
     * Whether the given config is supported by the FLV legacy format.
     *
     * @param config the video codec configuration
     * @return true if the config is supported by the FLV legacy format
     */
    fun isLegacyConfig(config: VideoCodecConfig): Boolean {
        return isLegacyVideoMimeType(config.mimeType)
    }
}
