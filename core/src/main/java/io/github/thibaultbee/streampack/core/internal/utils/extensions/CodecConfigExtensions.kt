package io.github.thibaultbee.streampack.core.internal.utils.extensions

import io.github.thibaultbee.streampack.core.internal.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.internal.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.internal.sources.audio.AudioSourceConfig
import io.github.thibaultbee.streampack.core.internal.sources.video.VideoSourceConfig

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
 * Merges [AudioCodecConfig] with [AudioSourceConfig].
 *
 * @param sourceConfig [AudioSourceConfig] to merge with
 * @return [AudioCodecConfig] merged with [AudioSourceConfig]
 */
fun AudioCodecConfig.mergeWith(sourceConfig: AudioSourceConfig): AudioCodecConfig {
    return copy(
        channelConfig = sourceConfig.channelConfig,
        sampleRate = sourceConfig.sampleRate,
        byteFormat = sourceConfig.byteFormat
    )
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
    require(dynamicRangeProfile.dynamicRange == sourceConfig.dynamicRangeProfile) {
        "Dynamic range profile must be the same: $dynamicRangeProfile != ${sourceConfig.dynamicRangeProfile}"
    }
    return (fps == sourceConfig.fps)
}

/**
 * Merges [VideoCodecConfig] with [VideoCodecConfig].
 *
 * @param sourceConfig [VideoCodecConfig] to merge with
 * @return [VideoCodecConfig] merged with [VideoCodecConfig]
 */
fun VideoCodecConfig.mergeWith(sourceConfig: VideoSourceConfig): VideoCodecConfig {
    require(dynamicRangeProfile.dynamicRange == sourceConfig.dynamicRangeProfile) {
        "Dynamic range profile must be the same: $dynamicRangeProfile != ${sourceConfig.dynamicRangeProfile}"
    }

    return copy(
        fps = sourceConfig.fps
    )
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
        dynamicRangeProfile = dynamicRangeProfile.dynamicRange
    )