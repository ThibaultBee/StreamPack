package io.github.thibaultbee.streampack.core.internal.processing.video.utils.extensions

import android.graphics.Matrix
import android.graphics.RectF
import io.github.thibaultbee.streampack.core.internal.processing.video.utils.TransformUtils.NORMALIZED_RECT

/**
 * Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect.
 */
val RectF.normalized: Matrix
    get() {
        val normalizedToBuffer = Matrix()
        normalizedToBuffer.setRectToRect(NORMALIZED_RECT, this, Matrix.ScaleToFit.FILL)
        return normalizedToBuffer
    }