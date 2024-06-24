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
package io.github.thibaultbee.streampack.core.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.DynamicRangeProfiles.STANDARD
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi

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
 * Get default camera id.
 *
 * If a back camera is available, returns the first back camera id.
 * If no back camera is available, returns the first camera id.
 *
 * @return default camera id
 */
val Context.defaultCameraId: String
    get() {
        val cameraList = this.cameraList
        if (cameraList.isEmpty()) {
            throw IllegalStateException("No camera available")
        }
        val backCameraList = this.backCameraList
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
val Context.cameraList: List<String>
    get() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.cameraIdList.toList()
    }


/**
 * Gets back camera id list.
 *
 * @return List of back camera ids
 */
val Context.backCameraList: List<String>
    get() = cameraList.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_BACK }

/**
 * Gets front camera id list.
 *
 * @return List of front camera ids
 */
val Context.frontCameraList: List<String>
    get() = cameraList.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_FRONT }

/**
 * Gets external camera id list.
 *
 * @return List of front camera ids
 */
val Context.externalCameraList: List<String>
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraList.filter { getFacingDirection(it) == CameraCharacteristics.LENS_FACING_EXTERNAL }
        } else {
            emptyList()
        }

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
 * Check if string is an external camera id
 *
 * @return true if string is a external camera id, otherwise false
 */
fun Context.isExternalCamera(cameraId: String) =
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
fun Context.isFlashAvailable(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        ?: false

/**
 * Gets supported auto white balance modes
 *
 * @param cameraId camera id
 * @return list of supported white balance modes.
 */
fun Context.getAutoWhiteBalanceModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 *  Get supported iso range
 *
 * @param cameraId camera id
 * @return the iso range
 */
fun Context.getSensitivityRange(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)

/**
 * Get if camera supports white balance metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun Context.getWhiteBalanceMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)

/**
 * Get supported auto exposure modes.
 *
 * @param cameraId camera id
 * @return list of supported auto focus modes
 */
fun Context.getAutoExposureModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 * Gets exposure range.
 *
 * @param cameraId camera id
 * @return exposure range.
 */
fun Context.getExposureRange(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)

/**
 * Get exposure compensation step.
 *
 * This is the unit for [getExposureRange]. For example, if this key has a value of 1/2, then a
 * setting of -2 for  [getExposureRange] means that the target EV offset for the auto-exposure
 * routine is -1 EV.
 *
 * @param cameraId camera id
 * @return exposure range.
 */
fun Context.getExposureStep(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)

/**
 * Get if camera supports exposure metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun Context.getExposureMaxMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)

/**
 * Gets zoom ratio range.
 *
 * @param cameraId camera id
 * @return zoom ratio range.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun Context.getZoomRatioRange(cameraId: String): Range<Float>? {
    return getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
}

/**
 * Gets max scaler zoom.
 *
 * @param cameraId camera id
 * @return max scaler zoom.
 */
fun Context.getScalerMaxZoom(cameraId: String): Float {
    return getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
        ?: 1.0f
}

/**
 * Get supported auto focus modes.
 *
 * @param cameraId camera id
 * @return list of supported auto focus modes
 */
fun Context.getAutoFocusModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 * Get supported lens distance range.
 *
 * @param cameraId camera id
 * @return lens distance range
 */
fun Context.getLensDistanceRange(cameraId: String) =
    Range(
        0f,
        getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            ?: 0f
    )

/**
 * Get if camera supports focus metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun Context.getFocusMaxMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)

/**
 * Checks if the camera supports optical stabilization.
 *
 * @param cameraId camera id
 * @return [Boolean.true] if camera supports optical stabilization, [Boolean.false] otherwise.
 */
fun Context.isOpticalStabilizationAvailable(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        ?.contains(
            CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
        ) ?: false


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
fun Context.getCameraOutputStreamSizes(
    cameraId: String,
    imageFormat: Int = ImageFormat.YUV_420_888
): List<Size> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        imageFormat
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
 * Whether the camera supports the capability.
 *
 * @param cameraId camera id
 * @return true if the camera supports the capability, false otherwise
 */
private fun Context.isCapabilitiesSupported(cameraId: String, capability: Int): Boolean {
    val availableCapabilities = this.getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    return availableCapabilities?.contains(capability) ?: false
}

/**
 * Whether the camera supports 10-bit dynamic range output.
 *
 * @param cameraId camera id
 * @return true if the camera supports 10-bit dynamic range output, false otherwise
 */
fun Context.is10BitProfileSupported(cameraId: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isCapabilitiesSupported(
            cameraId,
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
        )
    } else {
        false
    }
}

/**
 * Get list of 10-bit dynamic range output profiles.
 *
 * @param cameraId camera id
 * @return List of 10-bit dynamic range output profiles
 */
fun Context.get10BitSupportedProfiles(cameraId: String): Set<Long> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)?.supportedProfiles
            ?: setOf(STANDARD)
    } else {
        setOf(STANDARD)
    }
}
