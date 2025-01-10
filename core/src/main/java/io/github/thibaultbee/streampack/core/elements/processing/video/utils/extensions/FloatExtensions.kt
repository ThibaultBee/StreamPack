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
package io.github.thibaultbee.streampack.core.elements.processing.video.utils.extensions

import android.opengl.Matrix

/**
 * Preconcats the matrix with the specified rotation. M' = M * R(degrees, px, py)
 *
 *
 * The pivot point is the coordinate that should remain unchanged by the specified
 * transformation.
 *
 * @param matrix  the matrix to rotate
 * @param degrees the rotation degrees
 * @param px      px of pivot point at (px, py)
 * @param py      py of pivot point at (px, py)
 */
fun FloatArray.preRotate(degrees: Float, px: Float, py: Float) {
    normalize(px, py)
    Matrix.rotateM(this, 0, degrees, 0f, 0f, 1f)
    denormalize(px, py)
}

/**
 * Preconcats the matrix with a vertical flip operation.
 *
 * @param y      the horizontal line to flip along
 */
fun FloatArray.preVerticalFlip(y: Float) {
    normalize(0.toFloat(), y)
    Matrix.scaleM(this, 0, 1f, -1f, 1f)
    denormalize(0.toFloat(), y)
}

private fun FloatArray.normalize(px: Float, py: Float) {
    Matrix.translateM(this, 0, px, py, 0f)
}

private fun FloatArray.denormalize(px: Float, py: Float) {
    Matrix.translateM(this, 0, -px, -py, 0f)
}

