/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils

import android.media.AudioFormat
import android.media.MediaFormat
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import java.io.IOException

/**
 * Internal utilities to convert between StreamPack and krtmps types.
 */
internal object FlvUtils {
    internal fun audioMediaTypeFromMimeType(mimeType: String) = when {
        mimeType == MediaFormat.MIMETYPE_AUDIO_RAW -> AudioMediaType.PCM
        mimeType == MediaFormat.MIMETYPE_AUDIO_G711_ALAW -> AudioMediaType.G711_ALAW
        mimeType == MediaFormat.MIMETYPE_AUDIO_G711_MLAW -> AudioMediaType.G711_MLAW
        AudioCodecConfig.isAacMimeType(mimeType) -> AudioMediaType.AAC
        mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS -> AudioMediaType.OPUS
        else -> throw IOException("MimeType is not supported: $mimeType")
    }

    internal fun mimeTypeFromAudioMediaType(mediaType: AudioMediaType) = when (mediaType) {
        AudioMediaType.PCM -> MediaFormat.MIMETYPE_AUDIO_RAW
        AudioMediaType.G711_ALAW -> MediaFormat.MIMETYPE_AUDIO_G711_ALAW
        AudioMediaType.G711_MLAW -> MediaFormat.MIMETYPE_AUDIO_G711_MLAW
        AudioMediaType.AAC -> MediaFormat.MIMETYPE_AUDIO_AAC
        AudioMediaType.OPUS -> MediaFormat.MIMETYPE_AUDIO_OPUS
        else -> throw IOException("Media type is not supported: $mediaType")
    }

    internal fun isLegacyAudioMimeType(mimeType: String): Boolean {
        return (mimeType == MediaFormat.MIMETYPE_AUDIO_RAW) ||
                (mimeType == MediaFormat.MIMETYPE_AUDIO_G711_ALAW) ||
                (mimeType == MediaFormat.MIMETYPE_AUDIO_G711_MLAW) ||
                AudioCodecConfig.isAacMimeType(mimeType)
    }

    internal fun soundSizeFromByteFormat(byteFormat: Int) = when (byteFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> SoundSize.S_8BITS
        AudioFormat.ENCODING_PCM_16BIT -> SoundSize.S_16BITS
        else -> throw IllegalArgumentException("Unsupported byte format: $byteFormat")
    }

    internal fun byteFormatFromSoundSize(soundSize: SoundSize) = when (soundSize) {
        SoundSize.S_8BITS -> AudioFormat.ENCODING_PCM_8BIT
        SoundSize.S_16BITS -> AudioFormat.ENCODING_PCM_16BIT
    }

    internal fun isLegacyByteFormat(byteFormat: Int): Boolean {
        return (byteFormat == AudioFormat.ENCODING_PCM_8BIT) ||
                (byteFormat == AudioFormat.ENCODING_PCM_16BIT)
    }

    internal fun soundTypeFromChannelConfig(channelConfig: Int) = when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO -> SoundType.MONO
        AudioFormat.CHANNEL_IN_STEREO -> SoundType.STEREO
        else -> throw IllegalArgumentException("Unsupported channel configuration: $channelConfig")
    }

    internal fun videoMediaTypeFromMimeType(mimeType: String) = when (mimeType) {
        MediaFormat.MIMETYPE_VIDEO_H263 -> VideoMediaType.SORENSON_H263
        MediaFormat.MIMETYPE_VIDEO_AVC -> VideoMediaType.AVC
        MediaFormat.MIMETYPE_VIDEO_HEVC -> VideoMediaType.HEVC
        MediaFormat.MIMETYPE_VIDEO_VP8 -> VideoMediaType.VP8
        MediaFormat.MIMETYPE_VIDEO_VP9 -> VideoMediaType.VP9
        MediaFormat.MIMETYPE_VIDEO_AV1 -> VideoMediaType.AV1
        else -> throw IOException("MimeType is not supported: $mimeType")
    }

    internal fun mimeTypeFromVideoMediaType(mediaType: VideoMediaType) = when (mediaType) {
        VideoMediaType.SORENSON_H263 -> MediaFormat.MIMETYPE_VIDEO_H263
        VideoMediaType.AVC -> MediaFormat.MIMETYPE_VIDEO_AVC
        VideoMediaType.HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
        VideoMediaType.VP8 -> MediaFormat.MIMETYPE_VIDEO_VP8
        VideoMediaType.VP9 -> MediaFormat.MIMETYPE_VIDEO_VP9
        VideoMediaType.AV1 -> MediaFormat.MIMETYPE_VIDEO_AV1
        else -> throw IOException("Media type is not supported: $mediaType")
    }

    internal fun isLegacyVideoMimeType(mimeType: String): Boolean {
        return (mimeType == MediaFormat.MIMETYPE_VIDEO_H263) ||
                (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC)
    }
}
