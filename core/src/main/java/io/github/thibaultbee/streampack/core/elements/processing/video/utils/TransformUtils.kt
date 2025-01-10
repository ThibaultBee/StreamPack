package io.github.thibaultbee.streampack.core.elements.processing.video.utils

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.normalized


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
        val matrix = Matrix()
        matrix.setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL)
        // Add rotation.
        matrix.postRotate(rotationDegrees.toFloat())
        if (mirroring) {
            matrix.postScale(-1f, 1f)
        }
        // Restore the normalized space to target's coordinates.
        matrix.postConcat(target.normalized)
        return matrix
    }
}