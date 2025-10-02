package io.github.thibaultbee.streampack.core.elements.encoders.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.logger.Logger

internal object MediaCodecUtils {

    /**
     * Creates a [MediaCodec] for a given encoder config
     *
     * It try to create a MediaCodec with profile and level, if it fails, it fallback to a MediaCodec without profile and level.
     *
     * @param encoderConfig Encoder configuration
     * @return MediaCodec
     */
    internal fun createCodec(encoderConfig: EncoderConfig<*>): MediaCodecWithFormat {
        return try {
            try {
                createCodec(encoderConfig, true)
            } catch (t: Throwable) {
                Logger.i(TAG, "Fallback without profile and level (reason: $t)")
                createCodec(encoderConfig, false)
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to create encoder for ${encoderConfig.config}")
            throw t
        }
    }

    private fun createCodec(
        encoderConfig: EncoderConfig<*>,
        withProfileLevel: Boolean
    ): MediaCodecWithFormat {
        val format = encoderConfig.buildFormat(withProfileLevel)

        val encoderName = MediaCodecHelper.findEncoder(format)
        Logger.i(TAG, "Selected encoder $encoderName")
        return MediaCodecWithFormat(MediaCodec.createByCodecName(encoderName), format)
    }

    internal data class MediaCodecWithFormat(val mediaCodec: MediaCodec, val format: MediaFormat)

    private const val TAG = "EncoderUtils"
}