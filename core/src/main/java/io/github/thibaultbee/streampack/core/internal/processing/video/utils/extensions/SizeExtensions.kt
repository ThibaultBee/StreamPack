package io.github.thibaultbee.streampack.core.internal.processing.video.utils.extensions

import android.graphics.RectF
import android.util.Size

/**
 * Transforms size to a [RectF] with zero left and top.
 */
fun Size.toRectF(): RectF {
    return toRectF(0, 0)
}

/**
 * Transforms a size to a [RectF] with given left and top.
 */
fun Size.toRectF(left: Int, top: Int): RectF {
    return RectF(
        left.toFloat(),
        top.toFloat(),
        (left + width).toFloat(),
        (top + height).toFloat()
    )
}



