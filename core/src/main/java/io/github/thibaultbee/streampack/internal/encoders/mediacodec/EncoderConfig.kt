package io.github.thibaultbee.streampack.internal.encoders.mediacodec

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.orientation.ISourceOrientationProvider

sealed class EncoderConfig<T : Config>(val config: T) {
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
}

class VideoEncoderConfig(
    videoConfig: VideoConfig,
    val useSurfaceMode: Boolean = true,
    private val orientationProvider: ISourceOrientationProvider? = null
) : EncoderConfig<VideoConfig>(videoConfig) {
    override val isVideo = true
    override fun buildFormat(withProfileLevel: Boolean): MediaFormat {
        val format = config.getFormat(withProfileLevel)
        if (useSurfaceMode) {
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

    fun orientateFormat(format: MediaFormat) {
        orientationProvider?.let {
            it.getOrientedSize(config.resolution).apply {
                // Override previous format
                format.setInteger(MediaFormat.KEY_WIDTH, width)
                format.setInteger(MediaFormat.KEY_HEIGHT, height)
            }
        }
    }
}

class AudioEncoderConfig(audioConfig: AudioConfig) :
    EncoderConfig<AudioConfig>(audioConfig) {
    override val isVideo = false

    override fun buildFormat(withProfileLevel: Boolean) =
        config.getFormat(withProfileLevel)
}