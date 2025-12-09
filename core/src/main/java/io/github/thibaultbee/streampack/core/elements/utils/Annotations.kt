package io.github.thibaultbee.streampack.core.elements.utils

import android.hardware.camera2.CameraCharacteristics
import android.media.AudioFormat
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Process
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

/**
 * SDR color standard
 */
@IntDef(
    0, // Unspecified
    MediaFormat.COLOR_STANDARD_BT601_PAL,
    MediaFormat.COLOR_STANDARD_BT601_NTSC,
    MediaFormat.COLOR_STANDARD_BT709
)
@Retention(AnnotationRetention.SOURCE)
annotation class SdrColorStandardValue


/**
 * Valid process thread priority values
 */
@IntDef(
    Process.THREAD_PRIORITY_AUDIO,
    Process.THREAD_PRIORITY_BACKGROUND,
    Process.THREAD_PRIORITY_DEFAULT,
    Process.THREAD_PRIORITY_DISPLAY,
    Process.THREAD_PRIORITY_FOREGROUND,
    Process.THREAD_PRIORITY_LESS_FAVORABLE,
    Process.THREAD_PRIORITY_LOWEST,
    Process.THREAD_PRIORITY_MORE_FAVORABLE,
    Process.THREAD_PRIORITY_URGENT_AUDIO,
    Process.THREAD_PRIORITY_URGENT_DISPLAY,
    Process.THREAD_PRIORITY_VIDEO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ProcessThreadPriorityValue

/**
 * Valid thread priority values
 */
@IntDef(
    Thread.MIN_PRIORITY,
    Thread.NORM_PRIORITY,
    Thread.MAX_PRIORITY
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ThreadPriorityValue


/**
 * Valid lens facing direction values
 */
@IntDef(
    CameraCharacteristics.LENS_FACING_BACK,
    CameraCharacteristics.LENS_FACING_FRONT,
    CameraCharacteristics.LENS_FACING_EXTERNAL
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class Camera2FacingDirectionValue

/**
 * Valid audio source values
 */
@IntDef(
    MediaRecorder.AudioSource.CAMCORDER,
    MediaRecorder.AudioSource.DEFAULT,
    MediaRecorder.AudioSource.MIC,
    MediaRecorder.AudioSource.UNPROCESSED,
    MediaRecorder.AudioSource.VOICE_CALL,
    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    MediaRecorder.AudioSource.VOICE_DOWNLINK,
    MediaRecorder.AudioSource.VOICE_PERFORMANCE,
    MediaRecorder.AudioSource.VOICE_RECOGNITION,
    MediaRecorder.AudioSource.VOICE_UPLINK
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AudioSourceValue