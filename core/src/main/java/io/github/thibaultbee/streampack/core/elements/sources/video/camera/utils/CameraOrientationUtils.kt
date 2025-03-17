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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

object CameraOrientationUtils {
    /**
     * Calculates the delta between a source rotation and destination rotation.
     *
     * <p>A typical use of this method would be calculating the angular difference between the
     * display orientation (destRotationDegrees) and camera sensor orientation
     * (sourceRotationDegrees).
     *
     * @param destRotationDegrees   The destination rotation relative to the device's natural
     *                              rotation.
     * @param sourceRotationDegrees The source rotation relative to the device's natural rotation.
     * @param isOppositeFacing      Whether the source and destination planes are facing opposite
     *                              directions.
     */
    fun getRelativeRotation(
        destRotationDegrees: Int, sourceRotationDegrees: Int, isOppositeFacing: Boolean
    ): Int {
        return if (isOppositeFacing) {
            (sourceRotationDegrees - destRotationDegrees + 360) % 360
        } else {
            (sourceRotationDegrees + destRotationDegrees) % 360
        }
    }
}