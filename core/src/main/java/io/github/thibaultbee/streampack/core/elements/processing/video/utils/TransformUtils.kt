package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.normalized
import kotlin.math.abs


object TransformUtils {
    val NORMALIZED_RECT: RectF = RectF(-1f, -1f, 1f, 1f)

    /**
     * Gets the transform from one [RectF] to another with rotation degrees and mirroring.
     *
     *
     *  Following is how the source is mapped to the target with a 90° rotation and a mirroring.
     * The rect <a></a>, b, c, d> is mapped to <a></a>', b', c', d'>.
     *
     * <pre>
     * a----------b                           a'-----------d'
     * |  source  |    -90° + mirroring ->    |            |
     * d----------c                           |   target   |
     *                                        |            |
     *                                        b'-----------c'
    </pre> *
     */
    fun getRectToRect(
        source: RectF,
        target: RectF,
        @IntRange(from = 0, to = 359) rotationDegrees: Int,
        mirroring: Boolean
    ): Matrix {
        // Map source to normalized space.
        return Matrix().apply {
            setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL)
            postRotate(rotationDegrees.toFloat())
            if (mirroring) {
                postScale(-1f, 1f)
            }
            postConcat(target.normalized)
        }
    }

    fun calculateViewfinder(
        source: RectF,
        target: RectF
    ): RectF {
        val sourceAspectRatio = abs(source.width() / source.height())
        val targetAspectRatio = abs(target.width() / target.height())

        return when {
            sourceAspectRatio == targetAspectRatio -> {
                // Same aspect ratio, no need to adjust
                RectF(target)
            }
            sourceAspectRatio > targetAspectRatio -> {
                // Source is wider than target, fit width
                val scaledHeight = target.width() / sourceAspectRatio
                val top = target.centerY() - scaledHeight / 2f
                RectF(
                    target.left,
                    top,
                    target.right,
                    top + scaledHeight
                )
            }
            else -> {
                // Source is taller than target, fit height
                val scaledWidth = target.height() * sourceAspectRatio
                val left = target.centerX() - scaledWidth / 2f
                RectF(
                    left,
                    target.top,
                    left + scaledWidth,
                    target.bottom
                )
            }
        }
    }
}