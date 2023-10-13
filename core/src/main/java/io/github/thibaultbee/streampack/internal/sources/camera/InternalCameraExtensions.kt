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
package io.github.thibaultbee.streampack.internal.sources.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size
import io.github.thibaultbee.streampack.utils.cameraList
import io.github.thibaultbee.streampack.utils.getCameraCharacteristics

/**
 * Gets all output capture sizes.
 *
 * @return List of resolutions supported by all camera
 */
fun Context.getCameraOutputStreamSizes(): List<Size> {
    val cameraIdList = cameraList
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
 * Get list of framerate for a camera.
 *
 * @param cameraId camera id
 * @return List of fps supported by a camera
 */
fun Context.getCameraFpsList(cameraId: String): List<Range<Int>> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
        ?: emptyList()
}
