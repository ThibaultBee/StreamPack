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
package io.github.thibaultbee.streampack.core.internal.sources.video.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.internal.processing.video.source.AbstractSourceInfoProvider
import io.github.thibaultbee.streampack.core.internal.utils.RotationValue
import io.github.thibaultbee.streampack.core.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.core.internal.utils.extensions.rotationToDegrees
import io.github.thibaultbee.streampack.core.utils.extensions.getCameraCharacteristics
import io.github.thibaultbee.streampack.core.utils.extensions.getFacingDirection

class CameraInfoProvider(private val context: Context, initialCameraId: String) :
    AbstractSourceInfoProvider() {

    var cameraId: String = initialCameraId
        set(value) {
            if (field == value) {
                return
            }
            field = value
        }

    override val rotationDegrees: Int
        @IntRange(from = 0, to = 359)
        get() {
            val characteristics = context.getCameraCharacteristics(cameraId)
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        }

    val isFrontFacing: Boolean
        get() = context.getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_FRONT

    override val isMirror = false

    @IntRange(from = 0, to = 359)
    override fun getRelativeRotationDegrees(
        @RotationValue targetRotation: Int, requiredMirroring: Boolean
    ) = getSensorRotationDegrees(targetRotation)

    @IntRange(from = 0, to = 359)
    private fun getSensorRotationDegrees(@RotationValue targetRotation: Int): Int {
        val sensorOrientation = rotationDegrees

        val targetRotationDegrees = targetRotation.rotationToDegrees

        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        val lensFacing = context.getFacingDirection(cameraId)
        val isOppositeFacingScreen = CameraCharacteristics.LENS_FACING_BACK == lensFacing
        return CameraOrientationUtils.getRelativeRotation(
            targetRotationDegrees, sensorOrientation, isOppositeFacingScreen
        )
    }

    override fun getSurfaceSize(size: Size, targetRotation: Int) = size.landscapize
}