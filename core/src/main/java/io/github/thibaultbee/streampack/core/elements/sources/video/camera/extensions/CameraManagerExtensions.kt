/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import io.github.thibaultbee.streampack.core.elements.utils.Camera2FacingDirectionValue

/**
 * Gets default camera id.
 *
 * If a back camera is available, returns the first back camera id.
 * If no back camera is available, returns the first camera id.
 *
 * @return default camera id
 */
val CameraManager.defaultCameraId: String
    get() {
        val cameraList = this.cameras
        if (cameraList.isEmpty()) {
            throw IllegalStateException("No camera available")
        }
        val backCameraList = this.backCameras
        return if (backCameraList.isEmpty()) {
            cameraList.first()
        } else {
            backCameraList.first()
        }
    }

/**
 * Gets camera id list.
 *
 * @return List of camera ids
 */
val CameraManager.cameras: List<String>
    get() = cameraIdList.toList()


/**
 * Gets back camera id list.
 *
 * @return List of back camera ids
 */
val CameraManager.backCameras: List<String>
    get() = cameras.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_BACK }

/**
 * Gets front camera id list.
 *
 * @return List of front camera ids
 */
val CameraManager.frontCameras: List<String>
    get() = cameras.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_FRONT }

/**
 * Gets external camera id list.
 *
 * @return List of front camera ids
 */
val CameraManager.externalCameras: List<String>
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameras.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_EXTERNAL }
        } else {
            emptyList()
        }

/**
 * Whether the [cameraId] is a back camera id
 *
 * @return true if string is a back camera id, otherwise false
 */
fun CameraManager.isBackCamera(cameraId: String) =
    getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_BACK

/**
 * Whether the [cameraId] is a front camera id
 *
 * @return true if string is a front camera id, otherwise false
 */
fun CameraManager.isFrontCamera(cameraId: String) =
    getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_FRONT

/**
 * Whether the [cameraId] is an external camera id
 *
 * @return true if string is a external camera id, otherwise false
 */
fun CameraManager.isExternalCamera(cameraId: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_EXTERNAL
    } else {
        false
    }

/**
 * Gets camera facing direction.
 *
 * @param cameraId camera id
 * @return camera facing direction, either [CameraCharacteristics.LENS_FACING_BACK], [CameraCharacteristics.LENS_FACING_FRONT] or [CameraCharacteristics.LENS_FACING_EXTERNAL]
 */
@Camera2FacingDirectionValue
private fun CameraManager.getFacingDirection(cameraId: String) =
    getCameraCharacteristics(cameraId).facingDirection


/**
 * Gets all output capture sizes.
 *
 * @return List of resolutions supported by all camera
 */
fun CameraManager.getCameraOutputStreamSizes(): List<Size> {
    val cameraIdList = cameras
    val resolutionSet = mutableSetOf<Size>()
    cameraIdList.forEach { cameraId ->
        resolutionSet.addAll(getCameraCharacteristics(cameraId).getCameraOutputStreamSizes())
    }
    return resolutionSet.toList()
}