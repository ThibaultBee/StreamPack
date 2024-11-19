/*
 * Copyright 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils

import android.view.Surface
import androidx.annotation.IntRange

object OrientationUtils {
    /**
     * Returns the surface orientation in degrees from [Surface] rotation ([Surface.ROTATION_0], ...).
     */
    @IntRange(from = 0, to = 359)
    fun getSurfaceRotationDegrees(surfaceOrientation: Int): Int {
        return when (surfaceOrientation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalArgumentException("Invalid surface orientation: $surfaceOrientation")
        }
    }
}