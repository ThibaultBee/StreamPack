/*
 * Copyright 2026 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils

import android.graphics.PointF

/**
 * A factory to create a normalized [PointF] for tap-to-focus and metering.
 * The output point will have its x and y coordinates in the range [0, 1], representing
 * the relative position on the active video frame.
 */
abstract class MeteringPointFactory {
    /**
     * Converts a raw coordinate into a normalized [PointF].
     *
     * @param x the x coordinate in the view/surface space
     * @param y the y coordinate in the view/surface space
     * @return a normalized [PointF] with values between 0.0 and 1.0
     */
    fun createPoint(x: Float, y: Float): PointF {
        return translatePoint(x, y)
    }

    protected abstract fun translatePoint(x: Float, y: Float): PointF
}

/**
 * A [MeteringPointFactory] that assumes the raw coordinates map directly to a surface of
 * a specific width and height without any cropping or complex scaling.
 */
class SurfaceOrientedMeteringPointFactory(
    private val width: Float,
    private val height: Float
) : MeteringPointFactory() {
    override fun translatePoint(x: Float, y: Float): PointF {
        return PointF(x / width, y / height)
    }
}
