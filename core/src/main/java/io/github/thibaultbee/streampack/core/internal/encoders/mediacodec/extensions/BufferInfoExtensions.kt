package io.github.thibaultbee.streampack.core.internal.encoders.mediacodec.extensions

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import io.github.thibaultbee.streampack.core.logger.Logger


private const val TAG = "BufferInfoExtensions"

val BufferInfo.isValid: Boolean
    get() {
        if (size <= 0) {
            Logger.w(TAG, "Invalid buffer size: $size")
            return false
        }

        if (isCodecConfig) {
            Logger.d(TAG, "Drop buffer by codec config.")
            return false
        }
        return true
    }

/**
 * Whether if the buffer is a codec config buffer
 *
 * @return true if the buffer is a codec config buffer
 */
val BufferInfo.isCodecConfig: Boolean
    get() = flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

/**
 * Whether if the buffer is a key frame
 *
 * @return true if the buffer is a key frame
 */
val BufferInfo.isKeyFrame: Boolean
    get() = flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0

/**
 * Whether if the buffer is an end of stream buffer
 *
 * @return true if the buffer is an end of stream buffer
 */
val BufferInfo.hasEndOfStreamFlag: Boolean
    get() = flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0