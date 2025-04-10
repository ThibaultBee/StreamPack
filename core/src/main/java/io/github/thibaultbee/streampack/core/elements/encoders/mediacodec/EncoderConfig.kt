/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.EncoderMode
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig

sealed class EncoderConfig<T : CodecConfig>(val config: T, val mode: EncoderMode) {
    /**
     * True if the encoder is a video encoder, false if it's an audio encoder
     */
    abstract val isVideo: Boolean

    /**
     * Get media format for the encoder
     * @param withProfileLevel true if profile and level should be used
     * @return MediaFormat
     */
    abstract fun buildFormat(withProfileLevel: Boolean): MediaFormat

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncoderConfig<*>) return false

        if (config != other.config) return false

        return true
    }

    override fun hashCode(): Int {
        var result = config.hashCode()
        result = 31 * result + isVideo.hashCode()
        return result
    }
}

class VideoEncoderConfig(
    videoConfig: VideoCodecConfig,
    mode: EncoderMode = EncoderMode.SURFACE
) : EncoderConfig<VideoCodecConfig>(
    videoConfig,
    mode
) {
    override val isVideo = true

    override fun buildFormat(withProfileLevel: Boolean): MediaFormat {
        val format = config.getFormat(withProfileLevel)
        if (mode == EncoderMode.SURFACE) {
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
        } else {
            val colorFormat =
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && config.dynamicRangeProfile.isHdr) {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010
                } else {
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                }
            format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                colorFormat
            )
        }
        return format
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoEncoderConfig) return false

        if (!super.equals(other)) return false
        if (mode != other.mode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + isVideo.hashCode()
        return result
    }
}

class AudioEncoderConfig(audioConfig: AudioCodecConfig, mode: EncoderMode) :
    EncoderConfig<AudioCodecConfig>(
        audioConfig,
        mode
    ) {

    override val isVideo = false

    init {
        require(mode != EncoderMode.SURFACE) {
            "Audio encoder can't be in SURFACE mode"
        }
    }

    override fun buildFormat(withProfileLevel: Boolean) =
        config.getFormat(withProfileLevel)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioEncoderConfig) return false

        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isVideo.hashCode()
        return result
    }
}