package io.github.thibaultbee.streampack.core.internal.utils

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