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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.annotation.IntRange
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraOrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotationToDegrees

internal fun CameraInfoProvider(
    cameraManager: CameraManager,
    cameraId: String
): CameraInfoProvider {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val rotationDegrees = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    val facingDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
    return CameraInfoProvider(rotationDegrees, facingDirection = facingDirection)
}

class CameraInfoProvider(
    @IntRange(from = 0, to = 359) override val rotationDegrees: Int,
    private val facingDirection: Int?
) :
    ISourceInfoProvider {

    val isFrontFacing: Boolean = facingDirection == CameraCharacteristics.LENS_FACING_FRONT
    override val isMirror = isFrontFacing

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
        val isOppositeFacingScreen = CameraCharacteristics.LENS_FACING_BACK == facingDirection
        return CameraOrientationUtils.getRelativeRotation(
            targetRotationDegrees, sensorOrientation, isOppositeFacingScreen
        )
    }

    override fun getSurfaceSize(targetResolution: Size) = targetResolution

    override fun toString(): String {
        return "CameraInfoProvider(rotationDegrees=$rotationDegrees, isMirror=$isMirror, facingDirection=$facingDirection, isFrontFacing=$isFrontFacing)"
    }
}