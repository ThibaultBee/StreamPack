package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.normalized
import io.github.thibaultbee.streampack.core.elements.utils.Position
import io.github.thibaultbee.streampack.core.elements.utils.ScaleMode
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
        target: RectF,
        scaleMode: ScaleMode = ScaleMode.CROP,
        position: Position = Position.CENTER
    ): RectF {
        val sourceAspectRatio = abs(source.width() / source.height())
        val targetAspectRatio = abs(target.width() / target.height())

        if (sourceAspectRatio == targetAspectRatio) {
            return RectF(target)
        }

        val (scaledWidth, scaledHeight) = when (scaleMode) {
            ScaleMode.CROP -> {
                if (sourceAspectRatio > targetAspectRatio) {
                    target.width() to (target.width() / sourceAspectRatio)
                } else {
                    (target.height() * sourceAspectRatio) to target.height()
                }
            }
            ScaleMode.FIT -> {
                if (sourceAspectRatio > targetAspectRatio) {
                    (target.height() * sourceAspectRatio) to target.height()
                } else {
                    target.width() to (target.width() / sourceAspectRatio)
                }
            }
        }

        val excessWidth = target.width() - scaledWidth
        val left = when (position) {
            Position.START -> target.left
            Position.CENTER -> target.left + excessWidth / 2f
            Position.END -> target.left + excessWidth
        }

        val excessHeight = target.height() - scaledHeight
        val top = when (position) {
            Position.START -> target.top
            Position.CENTER -> target.top + excessHeight / 2f
            Position.END -> target.top + excessHeight
        }

        return RectF(left, top, left + scaledWidth, top + scaledHeight)
    }
}