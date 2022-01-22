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
package com.github.thibaultbee.streampack.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import com.github.thibaultbee.streampack.internal.sources.camera.getCameraFpsList

/**
 * Get camera characteristics.
 *
 * @param cameraId camera id
 * @return camera characteristics
 */
fun Context.getCameraCharacteristics(cameraId: String): CameraCharacteristics {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.getCameraCharacteristics(cameraId)
}

/**
 * Gets camera id list.
 *
 * @return List of camera ids
 */
fun Context.getCameraList(): List<String> {
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.cameraIdList.toList()
}

/**
 * Gets back camera id list.
 *
 * @return List of back camera ids
 */
fun Context.getBackCameraList() =
    getCameraList().filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_BACK }

/**
 * Gets back camera id list.
 *
 * @return List of front camera ids
 */
fun Context.getFrontCameraList() =
    getCameraList().filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_FRONT }

/**
 * Check if string is a back camera id
 *
 * @return true if string is a back camera id, otherwise false
 */
fun Context.isBackCamera(cameraId: String) =
    getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_BACK

/**
 * Check if string is a front camera id
 *
 * @return true if string is a front camera id, otherwise false
 */
fun Context.isFrontCamera(cameraId: String) =
    getFacingDirection(cameraId) == CameraCharacteristics.LENS_FACING_FRONT

/**
 * Gets camera facing direction.
 *
 * @param cameraId camera id
 * @return camera facing direction, either [CameraCharacteristics.LENS_FACING_BACK], [CameraCharacteristics.LENS_FACING_FRONT] or [CameraCharacteristics.LENS_FACING_EXTERNAL]
 */
fun Context.getFacingDirection(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)

/**
 * Gets list of output sizes compatible with [klass] of a camera.
 * Use it to select camera preview size.
 *
 * @param klass a non-null Class object reference (for example SurfaceHolder::class.java)
 * @param cameraId camera id
 * @return List of resolutions supported by a camera for the [klass]
 */
fun <T : Any> Context.getCameraOutputSizes(klass: Class<T>, cameraId: String): List<Size> {
    return getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        klass
    )?.toList() ?: emptyList()
}

/**
 * Checks if the camera supports a frame rate
 *
 * @param cameraId camera id
 * @param fps frame rate
 * @return [Boolean.true] if camera supports fps, [Boolean.false] otherwise.
 */
fun Context.isFrameRateSupported(cameraId: String, fps: Int) =
    getCameraFpsList(cameraId).any { it.contains(fps) }

/**
 * Checks if the camera has a flash device.
 *
 * @param cameraId camera id
 * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
 */
fun Context.isFlashAvailable(cameraId: String): Boolean =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        ?: false

/**
 * Gets supported auto white balance modes
 *
 * @param cameraId camera id
 * @return list of supported white balance mode.
 */
fun Context.getAutoWhiteBalanceModes(cameraId: String): List<Int> {
    return getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
        ?.toList() ?: emptyList()
}