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
package io.github.thibaultbee.streampack.core.streamers.dual

import android.media.AudioFormat
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaFormat
import android.util.Size
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig.Companion.getDefaultProfile
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.getBestLevel
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig.Companion.getBestProfile
import io.github.thibaultbee.streampack.core.elements.utils.ByteFormatValue
import io.github.thibaultbee.streampack.core.elements.utils.ChannelConfigValue
import io.github.thibaultbee.streampack.core.streamers.interfaces.IAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineAudioStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.single.AudioConfig
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig

/**
 * Creates a [DualStreamerAudioConfig] with the same configuration for both audio streams.
 *
 * @param config the audio configuration
 */
fun DualStreamerAudioConfig(config: AudioConfig) = DualStreamerAudioConfig(
    firstAudioConfig = config,
    secondAudioConfig = config
)

/**
 * Creates a [DualStreamerAudioConfig] with different configuration for each audio stream.
 */
fun DualStreamerAudioConfig(
    firstAudioCodecConfig: DualStreamerAudioCodecConfig = DualStreamerAudioCodecConfig(),
    secondAudioCodecConfig: DualStreamerAudioCodecConfig = DualStreamerAudioCodecConfig(),

    /**
     * Audio capture sample rate in Hz.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): "44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices."
     */
    sampleRate: Int = DualStreamerAudioCodecConfig.getDefaultSampleRate(
        listOf(
            firstAudioCodecConfig.mimeType,
            secondAudioCodecConfig.mimeType
        )
    ),

    /**
     * Audio channel configuration.
     * From [AudioRecord API](https://developer.android.com/reference/android/media/AudioRecord?hl=en#AudioRecord(int,%20int,%20int,%20int,%20int)): " AudioFormat#CHANNEL_IN_MONO is guaranteed to work on all devices."
     *
     * @see [AudioFormat.CHANNEL_IN_MONO]
     * @see [AudioFormat.CHANNEL_IN_STEREO]
     */
    @ChannelConfigValue channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,

    /**
     * Audio byte format.
     *
     * @see [AudioFormat.ENCODING_PCM_8BIT]
     * @see [AudioFormat.ENCODING_PCM_16BIT]
     * @see [AudioFormat.ENCODING_PCM_FLOAT]
     */
    @ByteFormatValue byteFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) = DualStreamerAudioConfig(
    firstAudioCodecConfig.toAudioCodecConfig(sampleRate, channelConfig, byteFormat),
    secondAudioCodecConfig.toAudioCodecConfig(sampleRate, channelConfig, byteFormat)
)

/**
 * A data class that holds audio specific codec data.
 */
data class DualStreamerAudioCodecConfig(
    /**
     * Audio encoder mime type.
     *
     * **See Also:** [MediaFormat MIMETYPE_AUDIO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String = MediaFormat.MIMETYPE_AUDIO_AAC,

    /**
     * Audio encoder bitrate in bits/s.
     */
    val startBitrate: Int = 128_000,

    /**
     * Audio profile.
     * For AAC only.
     * Default value is [MediaCodecInfo.CodecProfileLevel.AACObjectLC].
     *
     * @see [MediaCodecInfo.CodecProfileLevel.AACObjectLC]
     * @see [MediaCodecInfo.CodecProfileLevel.AACObjectHE]
     */
    val profile: Int = getDefaultProfile(mimeType),
) {
    /**
     * Creates an [AudioCodecConfig] from this [DualStreamerAudioCodecConfig].
     */
    internal fun toAudioCodecConfig(
        sampleRate: Int,
        @ChannelConfigValue channelConfig: Int,
        @ByteFormatValue byteFormat: Int
    ) = AudioCodecConfig(
        mimeType = mimeType,
        startBitrate = startBitrate,
        sampleRate = sampleRate,
        channelConfig = channelConfig,
        byteFormat = byteFormat,
        profile = profile
    )

    companion object {
        /**
         * Returns the default sample rate for the given mime types.
         */
        internal fun getDefaultSampleRate(mimeTypes: List<String>): Int {
            return if (mimeTypes.contains(MediaFormat.MIMETYPE_AUDIO_OPUS)) {
                48_000
            } else {
                44_100
            }
        }
    }
}

class DualStreamerAudioConfig
internal constructor(
    val firstAudioConfig: AudioCodecConfig,
    val secondAudioConfig: AudioCodecConfig
)

/**
 * Creates a [DualStreamerVideoConfig] with the same configuration for both video streams.
 *
 * @param config the video configuration
 */
fun DualStreamerVideoConfig(
    config: VideoConfig
) = DualStreamerVideoConfig(
    firstVideoConfig = config,
    secondVideoConfig = config
)

/**
 * Creates a [DualStreamerVideoConfig] with different configuration for each video stream.
 */
fun DualStreamerVideoConfig(
    /**
     * Video framerate.
     * This is a best effort as few camera can not generate a fixed framerate.
     */
    fps: Int = 30,
    firstVideoCodecConfig: DualStreamerVideoCodecConfig = DualStreamerVideoCodecConfig(),
    secondVideoCodecConfig: DualStreamerVideoCodecConfig = DualStreamerVideoCodecConfig()
) = DualStreamerVideoConfig(
    firstVideoCodecConfig.toVideoCodecConfig(fps),
    secondVideoCodecConfig.toVideoCodecConfig(fps)
)

/**
 * A data class that holds video specific codec data.
 */
data class DualStreamerVideoCodecConfig(
    /**
     * Video encoder mime type.
     * Only [MediaFormat.MIMETYPE_VIDEO_AVC], [MediaFormat.MIMETYPE_VIDEO_HEVC],
     * [MediaFormat.MIMETYPE_VIDEO_VP9] and [MediaFormat.MIMETYPE_VIDEO_AV1] are supported yet.
     *
     * **See Also:** [MediaFormat MIMETYPE_VIDEO_*](https://developer.android.com/reference/android/media/MediaFormat)
     */
    val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    /**
     * Video encoder bitrate in bits/s.
     */
    val startBitrate: Int = 2_000_000,
    /**
     * Video output resolution in pixel.
     */
    val resolution: Size = Size(1280, 720),
    /**
     * Video encoder profile. Encoders may not support requested profile. In this case, StreamPack fallbacks to default profile.
     * If not set, profile is always a 8 bit profile. StreamPack try to apply the highest profile available.
     * If the decoder does not support the profile, you should explicitly set the profile to a lower
     * value such as [AVCProfileBaseline] for AVC, [HEVCProfileMain] for HEVC, [VP9Profile0] for VP9.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val profile: Int = getBestProfile(mimeType),
    /**
     * Video encoder level. Encoders may not support requested level. In this case, StreamPack fallbacks to default level.
     * ** See ** [MediaCodecInfo.CodecProfileLevel](https://developer.android.com/reference/android/media/MediaCodecInfo.CodecProfileLevel)
     */
    val level: Int = getBestLevel(mimeType, profile),
    /**
     * Video encoder I-frame interval in seconds.
     * This is a best effort as few camera can not generate a fixed framerate.
     * For live streaming, I-frame interval should be really low. For recording, I-frame interval should be higher.
     * A value of 0 means that each frame is an I-frame.
     * On device with API < 25, this value will be rounded to an integer. So don't expect a precise value and any value < 0.5 will be considered as 0.
     */
    val gopDurationInS: Float = 1f  // 1s between I frames
) {
    internal fun toVideoCodecConfig(fps: Int) = VideoCodecConfig(
        mimeType = mimeType,
        startBitrate = startBitrate,
        resolution = resolution,
        fps = fps,
        profile = profile,
        level = level,
        gopDurationInS = gopDurationInS
    )
}

class DualStreamerVideoConfig
internal constructor(
    val firstVideoConfig: VideoCodecConfig,
    val secondVideoConfig: VideoCodecConfig
)

interface ICoroutineAudioDualStreamer : ICoroutineAudioStreamer<DualStreamerAudioConfig>,
    IAudioStreamer

interface ICoroutineVideoDualStreamer : ICoroutineVideoStreamer<DualStreamerVideoConfig>,
    IVideoStreamer

interface ICoroutineDualStreamer : ICoroutineStreamer