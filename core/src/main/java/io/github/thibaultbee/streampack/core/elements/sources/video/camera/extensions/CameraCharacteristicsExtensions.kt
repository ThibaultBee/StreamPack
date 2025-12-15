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
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.DynamicRangeProfiles.STANDARD
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.annotation.RequiresApi
import io.github.thibaultbee.streampack.core.elements.utils.Camera2FacingDirectionValue

/**
 * Whether the [CameraCharacteristics] is a back camera id
 *
 * @return true if string is a back camera id, otherwise false
 */
val CameraCharacteristics.isBackCamera: Boolean
    get() = facingDirection == CameraCharacteristics.LENS_FACING_BACK

/**
 * Whether the [CameraCharacteristics] is a front camera id
 *
 * @return true if string is a front camera id, otherwise false
 */
val CameraCharacteristics.isFrontCamera: Boolean
    get() = facingDirection == CameraCharacteristics.LENS_FACING_FRONT

/**
 * Whether the [CameraCharacteristics] is an external camera id
 *
 * @return true if string is a external camera id, otherwise false
 */
val CameraCharacteristics.isExternalCamera: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        facingDirection == CameraCharacteristics.LENS_FACING_EXTERNAL
    } else {
        false
    }

/**
 * Gets [CameraCharacteristics] facing direction.
 *
 * @return camera facing direction, either [CameraCharacteristics.LENS_FACING_BACK], [CameraCharacteristics.LENS_FACING_FRONT] or [CameraCharacteristics.LENS_FACING_EXTERNAL]
 */
val CameraCharacteristics.facingDirection: Int?
    @Camera2FacingDirectionValue
    get() = this[CameraCharacteristics.LENS_FACING]

/**
 * Gets list of output sizes compatible with [klass] of a camera.
 * Use it to select camera preview size.
 *
 * @param klass a non-null Class object reference (for example SurfaceHolder::class.java)
 * @return List of resolutions supported by a camera for the [klass]
 */
fun <T : Any> CameraCharacteristics.getCameraOutputSizes(klass: Class<T>): List<Size> {
    return this[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
        klass
    )?.toList() ?: emptyList()
}

/**
 * Whether the camera supports a frame rate
 *
 * @param fps frame rate
 * @return `true` if camera supports fps, `false` otherwise.
 */
fun CameraCharacteristics.isFpsSupported(fps: Int) =
    targetFps.any { it.contains(fps) }

/**
 * Whether the camera has a flash device.
 *
 * @return `true` if camera has a flash device, `false` otherwise.
 */
val CameraCharacteristics.isFlashAvailable: Boolean
    get() = this[CameraCharacteristics.FLASH_INFO_AVAILABLE] ?: false

/**
 * Gets supported auto white balance modes
 *
 * @return list of supported white balance modes.
 */
val CameraCharacteristics.autoWhiteBalanceModes: List<Int>
    get() = this[CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES]?.toList() ?: emptyList()

/**
 * Gets supported iso range
 *
 * @return the iso range
 */
val CameraCharacteristics.sensitivityRange: Range<Int>?
    get() = this[CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE]

/**
 * Gets the number of camera supported white balance metering regions.
 *
 * @return true if camera supports metering regions, false otherwise
 */
val CameraCharacteristics.maxNumberOfWhiteBalanceMeteringRegions: Int
    get() = this[CameraCharacteristics.CONTROL_MAX_REGIONS_AWB] ?: 0

/**
 * Gets supported auto exposure modes.
 *
 * @return list of supported auto focus modes
 */
val CameraCharacteristics.autoExposureModes: List<Int>
    get() = this[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]?.toList() ?: emptyList()

/**
 * Gets exposure range.
 *
 * @return exposure range.
 */
val CameraCharacteristics.exposureRange: Range<Int>?
    get() = this[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]

/**
 * Gets exposure compensation step.
 *
 * This is the unit for [exposureRange]. For example, if this key has a value of 1/2, then a
 * setting of -2 for  [exposureRange] means that the target EV offset for the auto-exposure
 * routine is -1 EV.
 *
 * @return exposure range.
 */
val CameraCharacteristics.exposureStep: Rational?
    get() = this[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]

/**
 * Gets the number of camera supported exposure metering regions.
 *
 * @return true if camera supports metering regions, false otherwise
 */
val CameraCharacteristics.maxNumberOfExposureMeteringRegions: Int
    get() = this[CameraCharacteristics.CONTROL_MAX_REGIONS_AE] ?: 0

/**
 * Gets zoom ratio range.
 *
 * @return zoom ratio range.
 */
val CameraCharacteristics.zoomRatioRange: Range<Float>?
    @RequiresApi(Build.VERSION_CODES.R)
    get() = this[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]

/**
 * Gets max scaler zoom.
 *
 * @return max scaler zoom.
 */
val CameraCharacteristics.scalerMaxZoom: Float
    get() = this[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM] ?: 1.0f

/**
 * Gets supported auto focus modes.
 *
 * @return list of supported auto focus modes
 */
val CameraCharacteristics.autoFocusModes: List<Int>
    get() = this[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]?.toList() ?: emptyList()

/**
 * Gets supported lens distance range.
 *
 * @return lens distance range
 */
val CameraCharacteristics.lensDistanceRange: Range<Float>
    get() = Range(
        0f,
        this[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]
            ?: 0f
    )

/**
 * Gets if camera supports focus metering regions.
 *
 * @return true if camera supports metering regions, false otherwise
 */
val CameraCharacteristics.maxNumberOfFocusMeteringRegions: Int?
    get() = this[CameraCharacteristics.CONTROL_MAX_REGIONS_AF]

/**
 * Whether the camera supports optical stabilization.
 *
 * @return `true` if camera supports optical stabilization, `false` otherwise.
 */
val CameraCharacteristics.isOpticalStabilizationAvailable: Boolean
    get() = this[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]?.contains(
        CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
    ) ?: false

/**
 * Gets list of output stream sizes of a camera.
 *
 * @return List of resolutions supported by a camera
 * @see [Context.getCameraOutputStreamSizes]
 */
fun CameraCharacteristics.getCameraOutputStreamSizes(
    imageFormat: Int = ImageFormat.YUV_420_888
): List<Size> {
    return this[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(imageFormat)
        ?.toList() ?: emptyList()
}

/**
 * Gets list of framerate for a camera.
 *
 * @return List of fps supported by a camera
 */
val CameraCharacteristics.targetFps: List<Range<Int>>
    get() = this[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
        ?: emptyList()

/**
 * Whether the camera supports the capability.
 *
 * @return true if the camera supports the capability, false otherwise
 */
fun CameraCharacteristics.isCapabilitiesSupported(
    capability: Int
): Boolean {
    val availableCapabilities = this[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
    return availableCapabilities?.contains(capability) ?: false
}

/**
 * Whether the camera supports 10-bit dynamic range output.
 *
 * @return true if the camera supports 10-bit dynamic range output, false otherwise
 */
val CameraCharacteristics.is10BitProfileSupported: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isCapabilitiesSupported(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
        )
    } else {
        false
    }

/**
 * Gets list of 10-bit dynamic range output profiles.
 *
 * @return List of 10-bit dynamic range output profiles
 */
val CameraCharacteristics.dynamicRangeProfiles: Set<Long>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this[CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES]?.supportedProfiles
            ?: setOf(STANDARD)
    } else {
        setOf(STANDARD)
    }
