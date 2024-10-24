package io.github.thibaultbee.streampack.core.internal.encoders.mediacodec

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.orientation.ISourceOrientationProvider

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
    videoConfig: VideoConfig,
    val useSurfaceMode: Boolean = true,
    private val orientationProvider: ISourceOrientationProvider? = null
) : EncoderConfig<VideoConfig>(
    videoConfig
) {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoEncoderConfig) return false

        if (!super.equals(other)) return false
        if (useSurfaceMode != other.useSurfaceMode) return false
        if (orientationProvider != other.orientationProvider) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + useSurfaceMode.hashCode()
        result = 31 * result + (orientationProvider?.hashCode() ?: 0)
        result = 31 * result + isVideo.hashCode()
        return result
    }
}

class AudioEncoderConfig(audioConfig: AudioConfig) :
    EncoderConfig<AudioConfig>(
        audioConfig
    ) {
    override val isVideo = false

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