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

import io.github.thibaultbee.krtmp.flv.config.FLVAudioConfig
import io.github.thibaultbee.krtmp.flv.config.FLVVideoConfig
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig

/**
 * Converts StreamPack [AudioCodecConfig] to krtmp [FLVAudioConfig].
 */
internal fun AudioCodecConfig.toFLVConfig(): FLVAudioConfig {
    return FLVAudioConfig(
        FlvUtils.audioMediaTypeFromMimeType(mimeType),
        startBitrate,
        sampleRate,
        AudioCodecConfig.getNumOfBytesPerSample(byteFormat) * Byte.SIZE_BITS,
        AudioCodecConfig.getNumberOfChannels(channelConfig)
    )
}

/**
 * Converts StreamPack [VideoCodecConfig] to krtmp [FLVVideoConfig].
 */
internal fun VideoCodecConfig.toFLVConfig(): FLVVideoConfig {
    return FLVVideoConfig(
        FlvUtils.videoMediaTypeFromMimeType(mimeType),
        startBitrate,
        resolution.width,
        resolution.height,
        fps
    )
}