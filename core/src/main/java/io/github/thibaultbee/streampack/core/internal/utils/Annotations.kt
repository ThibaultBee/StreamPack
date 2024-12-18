package io.github.thibaultbee.streampack.core.internal.utils

import android.media.AudioFormat
import android.view.Surface
import androidx.annotation.IntDef

/**
 * Valid integer rotation values.
 */
@IntDef(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class RotationValue

/**
 * Valid channel config
 */
@IntDef(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ChannelConfigValue

/**
 * Valid channel config
 */
@IntDef(
    AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ByteFormatValue
