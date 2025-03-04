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
package io.github.thibaultbee.streampack.core.elements.utils.extensions

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig

/**
 * Whether [AudioSourceConfig] is compatible with [AudioSourceConfig].
 *
 * @param sourceConfig [AudioSourceConfig] to compare with
 * @return `true` if [AudioSourceConfig] is compatible with [AudioSourceConfig], `false` otherwise
 */
fun AudioSourceConfig.isCompatibleWith(sourceConfig: AudioSourceConfig): Boolean {
    return (channelConfig == sourceConfig.channelConfig)
            && (sampleRate == sourceConfig.sampleRate)
            && (byteFormat == sourceConfig.byteFormat)
}

/**
 * Whether [AudioCodecConfig] is compatible with [AudioSourceConfig].
 *
 * @param sourceConfig [AudioSourceConfig] to compare with
 * @return `true` if [AudioCodecConfig] is compatible with [AudioSourceConfig], `false` otherwise
 */
fun AudioCodecConfig.isCompatibleWith(sourceConfig: AudioSourceConfig): Boolean {
    return (channelConfig == sourceConfig.channelConfig)
            && (sampleRate == sourceConfig.sampleRate)
            && (byteFormat == sourceConfig.byteFormat)
}

/**
 * Converts [AudioCodecConfig] to [AudioSourceConfig].
 *
 * @return [AudioSourceConfig] from [AudioCodecConfig]
 */
val AudioCodecConfig.sourceConfig: AudioSourceConfig
    get() = AudioSourceConfig(
        channelConfig = channelConfig,
        sampleRate = sampleRate,
        byteFormat = byteFormat
    )

/**
 * Whether [VideoSourceConfig] is compatible with [VideoSourceConfig].
 *
 * @param sourceConfig [VideoSourceConfig] to compare with
 * @return `true` if [VideoSourceConfig] is compatible with [VideoSourceConfig], `false` otherwise
 */
fun VideoSourceConfig.isCompatibleWith(sourceConfig: VideoSourceConfig): Boolean {
    return (fps == sourceConfig.fps) && (dynamicRangeProfile == sourceConfig.dynamicRangeProfile)
}

/**
 * Whether [VideoCodecConfig] is compatible with [VideoCodecConfig].
 *
 * @param sourceConfig [VideoCodecConfig] to compare with
 * @return `true` if [VideoCodecConfig] is compatible with [VideoCodecConfig], `false` otherwise
 */
fun VideoCodecConfig.isCompatibleWith(sourceConfig: VideoSourceConfig): Boolean {
    return (fps == sourceConfig.fps) && (dynamicRangeProfile == sourceConfig.dynamicRangeProfile)
}

/**
 * Converts [VideoCodecConfig] to [VideoSourceConfig].
 *
 * @return [VideoSourceConfig] from [VideoCodecConfig]
 */
val VideoCodecConfig.sourceConfig: VideoSourceConfig
    get() = VideoSourceConfig(
        resolution = resolution,
        fps = fps,
        dynamicRangeProfile = dynamicRangeProfile
    )