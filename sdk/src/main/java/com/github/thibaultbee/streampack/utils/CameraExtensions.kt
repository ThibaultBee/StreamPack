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

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresPermission


@RequiresPermission(Manifest.permission.CAMERA)
fun Context.getCameraCharacteristics(cameraId: String): CameraCharacteristics {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.getCameraCharacteristics(cameraId)
}

@RequiresPermission(Manifest.permission.CAMERA)
fun Context.getCameraList(): List<String> {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.cameraIdList.toList()
}

/**
 * Get output capture sizes supported by each camera
 * @return List of resolution supported by each camera
 */
@RequiresPermission(Manifest.permission.CAMERA)
fun Context.getOutputCaptureSizesIntersection(): List<Size> {
    val cameraIdList = this.getCameraList()
    var resolutionList = getOutputCaptureSizes(cameraIdList[0])
    cameraIdList.drop(1).forEach { cameraId ->
        val newResolutionList = getOutputCaptureSizes(cameraId)
        resolutionList = resolutionList.intersect(newResolutionList).toList()
    }
    return resolutionList
}

@RequiresPermission(Manifest.permission.CAMERA)
fun Context.getOutputCaptureSizes(cameraId: String): List<Size> {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        ImageFormat.YUV_420_888
    )?.toList() ?: emptyList()
}

@RequiresPermission(Manifest.permission.CAMERA)
fun <T : Any> Context.getOutputSizes(klass: Class<T>, cameraId: String): List<Size> {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        klass
    )?.toList() ?: emptyList()
}

@RequiresPermission(Manifest.permission.CAMERA)
fun Context.getFpsList(cameraId: String): List<Range<Int>> {
    val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
        ?: emptyList()
}