package io.github.thibaultbee.streampack.core.elements.utils.extensions

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig

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