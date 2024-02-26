package io.github.thibaultbee.streampack.internal.encoders.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import io.github.thibaultbee.streampack.logger.Logger

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
            } catch (e: Exception) {
                Logger.i(TAG, "Fallback without profile and level (reason: $e)")
                createCodec(encoderConfig, false)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create encoder for ${encoderConfig.config}")
            throw e
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

    internal class MediaCodecWithFormat(val mediaCodec: MediaCodec, val format: MediaFormat)

    private const val TAG = "EncoderUtils"
}