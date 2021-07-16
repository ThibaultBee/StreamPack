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
package com.github.thibaultbee.streampack.internal.sources.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.WindowManager

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
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.cameraIdList.toList()
}

/**
 * Gets all output capture sizes.
 *
 * @return List of resolutions supported by all camera
 */
fun Context.getCameraOutputStreamSizes(): List<Size> {
    val cameraIdList = this.getCameraList()
    val resolutionSet = mutableSetOf<Size>()
    cameraIdList.forEach { cameraId ->
        resolutionSet.addAll(getCameraOutputStreamSizes(cameraId))
    }
    return resolutionSet.toList()
}

/**
 * Gets list of output stream sizes of a camera.
 *
 * @param cameraId camera id
 * @return List of resolutions supported by a camera
 * @see [Context.getCameraOutputStreamSizes]
 */
fun Context.getCameraOutputStreamSizes(cameraId: String): List<Size> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        ImageFormat.YUV_420_888
    )?.toList() ?: emptyList()
}

/**
 * Gets list of output sizes compatible with [klass] of a camera.
 * Use it to select camera preview size.
 *
 * @param klass a non-null Class object reference (for example SurfaceHolder::class.java)
 * @param cameraId camera id
 * @return List of resolutions supported by a camera for the [klass]
 */
fun <T : Any> Context.getCameraOutputSizes(klass: Class<T>, cameraId: String): List<Size> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        klass
    )?.toList() ?: emptyList()
}

/**
 * Get list of framerate for a camera.
 *
 * @param cameraId camera id
 * @return List of fps supported by a camera
 */
fun Context.getCameraFpsList(cameraId: String): List<Range<Int>> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
        ?: emptyList()
}

/**
 * Returns the camera orientation.
 *
 * @return an integer equals to the current camera orientation
 */
fun Context.getCameraOrientation(): Int {
    val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return when (val displayRotation = windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_0 -> 90
        Surface.ROTATION_90 -> 0
        Surface.ROTATION_180 -> 270
        Surface.ROTATION_270 -> 180
        else -> throw UnsupportedOperationException(
            "Unsupported display rotation: $displayRotation"
        )
    }
}

/**
 * Check if camera is in portrait.
 *
 * @return true if camera is in portrait, otherwise false
 */
fun Context.isCameraPortrait(): Boolean {
    val orientation = this.getCameraOrientation()
    return orientation == 90 || orientation == 270
}