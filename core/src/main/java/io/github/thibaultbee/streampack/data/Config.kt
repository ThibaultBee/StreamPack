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
package io.github.thibaultbee.streampack.data

import android.media.MediaFormat
import io.github.thibaultbee.streampack.internal.encoders.mediacodec.MediaCodecHelper
import io.github.thibaultbee.streampack.streamers.DefaultStreamer

/**
 * Base configuration class.
 * If you don't know how to set class members, [Video encoding recommendations](https://developer.android.com/guide/topics/media/media-formats#video-encoding) should give you hints.
 *
 * @see [DefaultStreamer.configure]
 */
open class Config(
    /**
     * The encoder mime type.
     * Only [MediaFormat.MIMETYPE_AUDIO_AAC] is supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String,

    /**
     * The encoder bitrate in bits/s.
     */
    val startBitrate: Int,

    /**
     * The encoder profile.
     * Only applicable to AAC, AVC, HEVC, VP9, AV1.
     */
    val profile: Int = 0
) {
    /**
     * Get the media format from the configuration
     *
     * @return the corresponding media format
     */
    internal open fun getFormat(withProfileLevel: Boolean = true): MediaFormat {
        return MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mimeType)
            setInteger(MediaFormat.KEY_BIT_RATE, startBitrate)
        }
    }

    /**
     * Check if this configuration is supported by the default encoder.
     * If format is not supported, it won't be possible to start a stream.
     *
     * @return true if format is supported, otherwise false
     */
    val isFormatSupported: Boolean by lazy {
        if (MediaCodecHelper.isFormatSupported(getFormat(true))) {
            true
        } else {
            MediaCodecHelper.isFormatSupported(getFormat(false))
        }
    }

    /**
     * Check if this configuration is supported by the specified encoder.
     * If format is not supported, it won't be possible to start a stream.
     *
     * @param name the encoder name
     * @return true if format is supported, otherwise false
     */
    fun isFormatSupportedForEncoder(name: String): Boolean {
        return if (MediaCodecHelper.isFormatSupported(getFormat(true), name)) {
            true
        } else {
            MediaCodecHelper.isFormatSupported(getFormat(false), name)
        }
    }

    /**
     * Get default encoder name.
     * If name is null, it won't be possible to start a stream.
     *
     * @return the default encoder name
     */
    val defaultEncoderName: String? by lazy {
        try {
            MediaCodecHelper.findEncoder(getFormat(true))
        } catch (_: Exception) {
            try {
                MediaCodecHelper.findEncoder(getFormat(false))
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun toString() = "Config(mimeType='$mimeType', startBitrate=$startBitrate)"
}
