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
fun CameraManager.getFacingDirection(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)

/**
 * Gets list of output sizes compatible with [klass] of a camera.
 * Use it to select camera preview size.
 *
 * @param klass a non-null Class object reference (for example SurfaceHolder::class.java)
 * @param cameraId camera id
 * @return List of resolutions supported by a camera for the [klass]
 */
fun <T : Any> CameraManager.getCameraOutputSizes(klass: Class<T>, cameraId: String): List<Size> {
    return getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        klass
    )?.toList() ?: emptyList()
}

/**
 * Whether the camera supports a frame rate
 *
 * @param cameraId camera id
 * @param fps frame rate
 * @return [Boolean.true] if camera supports fps, [Boolean.false] otherwise.
 */
fun CameraManager.isFrameRateSupported(cameraId: String, fps: Int) =
    getCameraFps(cameraId).any { it.contains(fps) }

/**
 * Whether the camera has a flash device.
 *
 * @param cameraId camera id
 * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
 */
fun CameraManager.isFlashAvailable(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        ?: false

/**
 * Gets supported auto white balance modes
 *
 * @param cameraId camera id
 * @return list of supported white balance modes.
 */
fun CameraManager.getAutoWhiteBalanceModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 * Gets supported iso range
 *
 * @param cameraId camera id
 * @return the iso range
 */
fun CameraManager.getSensitivityRange(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)

/**
 * Gets the number of camera supported white balance metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun CameraManager.getWhiteBalanceMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)

/**
 * Gets supported auto exposure modes.
 *
 * @param cameraId camera id
 * @return list of supported auto focus modes
 */
fun CameraManager.getAutoExposureModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 * Gets exposure range.
 *
 * @param cameraId camera id
 * @return exposure range.
 */
fun CameraManager.getExposureRange(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)

/**
 * Gets exposure compensation step.
 *
 * This is the unit for [getExposureRange]. For example, if this key has a value of 1/2, then a
 * setting of -2 for  [getExposureRange] means that the target EV offset for the auto-exposure
 * routine is -1 EV.
 *
 * @param cameraId camera id
 * @return exposure range.
 */
fun CameraManager.getExposureStep(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)

/**
 * Gets the number of camera supported exposure metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun CameraManager.getExposureMaxMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)

/**
 * Gets zoom ratio range.
 *
 * @param cameraId camera id
 * @return zoom ratio range.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun CameraManager.getZoomRatioRange(cameraId: String): Range<Float>? {
    return getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
}

/**
 * Gets max scaler zoom.
 *
 * @param cameraId camera id
 * @return max scaler zoom.
 */
fun CameraManager.getScalerMaxZoom(cameraId: String): Float {
    return getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
        ?: 1.0f
}

/**
 * Gets supported auto focus modes.
 *
 * @param cameraId camera id
 * @return list of supported auto focus modes
 */
fun CameraManager.getAutoFocusModes(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        ?.toList() ?: emptyList()

/**
 * Gets supported lens distance range.
 *
 * @param cameraId camera id
 * @return lens distance range
 */
fun CameraManager.getLensDistanceRange(cameraId: String) =
    Range(
        0f,
        getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            ?: 0f
    )

/**
 * Gets if camera supports focus metering regions.
 *
 * @param cameraId camera id
 * @return true if camera supports metering regions, false otherwise
 */
fun CameraManager.getFocusMaxMeteringRegionsSupported(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)

/**
 * Whether the camera supports optical stabilization.
 *
 * @param cameraId camera id
 * @return [Boolean.true] if camera supports optical stabilization, [Boolean.false] otherwise.
 */
fun CameraManager.isOpticalStabilizationAvailable(cameraId: String) =
    getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        ?.contains(
            CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
        ) ?: false


/**
 * Gets all output capture sizes.
 *
 * @return List of resolutions supported by all camera
 */
fun CameraManager.getCameraOutputStreamSizes(): List<Size> {
    val cameraIdList = cameras
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
fun CameraManager.getCameraOutputStreamSizes(
    cameraId: String,
    imageFormat: Int = ImageFormat.YUV_420_888
): List<Size> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        imageFormat
    )?.toList() ?: emptyList()
}

/**
 * Gets list of framerate for a camera.
 *
 * @param cameraId camera id
 * @return List of fps supported by a camera
 */
fun CameraManager.getCameraFps(cameraId: String): List<Range<Int>> {
    return this.getCameraCharacteristics(cameraId)[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
        ?: emptyList()
}

/**
 * Whether the camera supports the capability.
 *
 * @param cameraId camera id
 * @return true if the camera supports the capability, false otherwise
 */
private fun CameraManager.isCapabilitiesSupported(cameraId: String, capability: Int): Boolean {
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
fun CameraManager.is10BitProfileSupported(cameraId: String): Boolean {
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
 * Gets list of 10-bit dynamic range output profiles.
 *
 * @param cameraId camera id
 * @return List of 10-bit dynamic range output profiles
 */
fun CameraManager.get10BitSupportedProfiles(cameraId: String): Set<Long> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)?.supportedProfiles
            ?: setOf(STANDARD)
    } else {
        setOf(STANDARD)
    }
}
