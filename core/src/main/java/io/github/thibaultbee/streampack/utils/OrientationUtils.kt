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
package io.github.thibaultbee.streampack.utils

import android.view.Surface

object OrientationUtils {
    fun getSurfaceOrientation(surfaceOrientation: Int): Int {
        return when (surfaceOrientation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    /**
     * Returns true if the rotation degrees is 90 or 270.
     */
    fun isPortrait(rotationDegrees: Int): Boolean {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return true
        }
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return false
        }
        throw IllegalArgumentException("Invalid rotation degrees: $rotationDegrees")
    }
}