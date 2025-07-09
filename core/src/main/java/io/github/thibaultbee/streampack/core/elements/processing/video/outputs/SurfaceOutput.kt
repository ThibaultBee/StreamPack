/*
 * Copyright (C) 2024 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.processing.video.outputs

import android.graphics.Rect
import android.graphics.RectF
import android.opengl.Matrix
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.ViewPortUtils.calculateViewportRect
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.TransformUtils
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.preRotate
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.preVerticalFlip
import io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions.toRectF
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor

class SurfaceOutput(
    override val descriptor: SurfaceDescriptor,
    override val isStreaming: () -> Boolean,
    private val transformationInfo: TransformationInfo
) :
    ISurfaceOutput {
    private val resolution = descriptor.resolution
    
    private val infoProvider: ISourceInfoProvider
        get() = transformationInfo.infoProvider

    @IntRange(from = 0, to = 359)
    val rotationDegrees = infoProvider.getRelativeRotationDegrees(
        transformationInfo.targetRotation,
        transformationInfo.needMirroring
    )

    @IntRange(from = 0, to = 359)
    val sourceRotationDegrees = infoProvider.rotationDegrees

    private val additionalTransform = FloatArray(16)
    private val invertedTextureTransform = FloatArray(16)

    override val viewportRect = calculateViewportRect(
        transformationInfo.aspectRatioMode,
        transformationInfo.infoProvider.getSurfaceSize(resolution),
        resolution
    )

    init {
        calculateAdditionalTransform(
            additionalTransform,
            invertedTextureTransform,
            transformationInfo.infoProvider
        )
    }

    override fun updateTransformMatrix(output: FloatArray, input: FloatArray) {
        Matrix.multiplyMM(
            output, 0, input, 0, additionalTransform, 0
        )
    }

    /**
     * Calculates the additional GL transform and saves it to additionalTransform.
     *
     *
     * The effect implementation needs to apply this value on top of texture transform obtained
     * from [SurfaceTexture.getTransformMatrix].
     *
     *
     * The overall transformation (A * B) is a concatenation of 2 values: A) the texture
     * transform (value of SurfaceTexture#getTransformMatrix), and B) CameraX's additional
     * transform based on user config such as the ViewPort API and UseCase#targetRotation. To
     * calculate B, we do it in 3 steps:
     *
     *  1. 1. Calculate A * B by using CameraX transformation value such as crop rect, relative
     * rotation, and mirroring. It already contains the texture transform(A).
     *  1. 2. Calculate A^-1 by predicating the texture transform(A) based on camera
     * characteristics then inverting it.
     *  1. 3. Calculate B by multiplying A^-1 * A * B.
     *
     */
    private fun calculateAdditionalTransform(
        additionalTransform: FloatArray,
        invertedTransform: FloatArray,
        sourceInfoProvider: ISourceInfoProvider
    ) {
        Matrix.setIdentityM(additionalTransform, 0)

        // Step 1, calculate the overall transformation(A * B) with the following steps:
        // - Flip compensate the GL coordinates v.s. image coordinates
        // - Rotate the image based on the relative rotation
        // - Mirror the image if needed
        // - Apply the crop rect

        // Flipping for GL.
        additionalTransform.preVerticalFlip(0.5f)

        // Rotation
        additionalTransform.preRotate(rotationDegrees.toFloat(), 0.5f, 0.5f)

        // Mirroring
        if (transformationInfo.needMirroring) {
            Matrix.translateM(additionalTransform, 0, 1f, 0f, 0f)
            Matrix.scaleM(additionalTransform, 0, -1f, 1f, 1f)
        }

        // Crop
        // Rotate the size and cropRect, and mirror the cropRect.
        val rotatedSize = resolution.rotate(
            rotationDegrees
        )
        val imageTransform = TransformUtils.getRectToRect(
            resolution.toRectF(),
            rotatedSize.toRectF(),
            rotationDegrees,
            sourceInfoProvider.isMirror
        )
        val rotatedCroppedRect = RectF(transformationInfo.cropRect)
        imageTransform.mapRect(rotatedCroppedRect)
        // According to the rotated size and cropRect, compute the normalized offset and the scale
        // of X and Y.
        val offsetX: Float = rotatedCroppedRect.left / rotatedSize.width
        val offsetY: Float = ((rotatedSize.height - rotatedCroppedRect.height()
                - rotatedCroppedRect.top)) / rotatedSize.height
        val scaleX: Float = rotatedCroppedRect.width() / rotatedSize.width
        val scaleY: Float = rotatedCroppedRect.height() / rotatedSize.height
        // Move to the new left-bottom position and apply the scale.
        Matrix.translateM(additionalTransform, 0, offsetX, offsetY, 0f)
        Matrix.scaleM(additionalTransform, 0, scaleX, scaleY, 1f)

        // Step 2: calculate the inverted texture transform: A^-1
        calculateInvertedTextureTransform(invertedTransform, sourceInfoProvider)

        // Step 3: calculate the additional transform: B = A^-1 * A * B
        Matrix.multiplyMM(
            additionalTransform, 0, invertedTransform, 0,
            additionalTransform, 0
        )
    }

    /**
     * Calculates the inverted texture transform and saves it to invertedTextureTransform.
     *
     * This method predicts the value of [SurfaceTexture.getTransformMatrix] based on
     * camera characteristics then invert it. The result is used to remove the texture transform
     * from overall transformation.
     */
    private fun calculateInvertedTextureTransform(
        invertedTextureTransform: FloatArray,
        sourceInfoProvider: ISourceInfoProvider
    ) {
        Matrix.setIdentityM(invertedTextureTransform, 0)

        // Flip for GL. SurfaceTexture#getTransformMatrix always contains this flipping regardless
        // of whether it has the camera transform.
        invertedTextureTransform.preVerticalFlip(0.5f)

        // Applies the camera sensor orientation if the input surface contains camera transform.
        // Rotation
        invertedTextureTransform.preRotate(
            sourceRotationDegrees.toFloat(),
            0.5f,
            0.5f
        )

        // Mirroring
        if (sourceInfoProvider.isMirror) {
            Matrix.translateM(invertedTextureTransform, 0, 1f, 0f, 0f)
            Matrix.scaleM(invertedTextureTransform, 0, -1f, 1f, 1f)
        }

        // Invert the matrix so it can be used to "undo" the SurfaceTexture#getTransformMatrix.
        Matrix.invertM(invertedTextureTransform, 0, invertedTextureTransform, 0)
    }

    data class TransformationInfo(
        val aspectRatioMode: AspectRatioMode,
        @RotationValue val targetRotation: Int,
        val cropRect: Rect,
        val needMirroring: Boolean,
        val infoProvider: ISourceInfoProvider
    )
}