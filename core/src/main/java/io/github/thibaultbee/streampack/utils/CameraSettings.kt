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
package io.github.thibaultbee.streampack.utils

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Rational
import io.github.thibaultbee.streampack.internal.sources.camera.CameraController
import io.github.thibaultbee.streampack.internal.utils.clamp
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer


/**
 * Use to change camera settings.
 * This object is returned by [BaseCameraStreamer.settings.camera].
 */
class CameraSettings(context: Context, cameraController: CameraController) {
    /**
     * Current camera flash API.
     */
    val flash = Flash(context, cameraController)

    /**
     * Current camera white balance API.
     */
    val whiteBalance = WhiteBalance(context, cameraController)

    /**
     * Current camera exposure API.
     */
    val exposure = Exposure(context, cameraController)

    /**
     * Current camera zoom API.
     */
    val zoom = Zoom(context, cameraController)

    /**
     * Current focus API.
     */
    val focus = Focus(context, cameraController)

    /**
     * Current stabilization API.
     */
    val stabilization = Stabilization(context, cameraController)
}

class WhiteBalance(private val context: Context, private val cameraController: CameraController) {
    /**
     * Gets supported auto white balance modes for the current camera
     *
     * @return list of supported white balance modes.
     */
    val availableAutoModes: List<Int>
        get() = cameraController.cameraId?.let { context.getAutoWhiteBalanceModes(it) }
            ?: emptyList()

    /**
     * Set or get auto white balance mode.
     *
     * **See Also:** [CONTROL_AWB_MODE](https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#CONTROL_AWB_MODE)
     * @see [availableAutoModes]
     */
    var autoMode: Int
        /**
         * Get auto white balance mode.
         *
         * @return current camera audo white balance mode
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AWB_MODE)
            ?: CaptureResult.CONTROL_AWB_MODE_OFF
        /**
         * Get auto white balance mode.
         *
         * @param value auto white balance mode
         */
        set(value) {
            cameraController.setSetting(CaptureRequest.CONTROL_AWB_MODE, value)
        }
}

class Flash(private val context: Context, private val cameraController: CameraController) {
    /**
     * Checks if the current camera has a flash device.
     *
     * @return [Boolean.true] if camera has a flash device, [Boolean.false] otherwise.
     */
    val available: Boolean
        get() = cameraController.cameraId?.let { context.isFlashAvailable(it) } ?: false

    /**
     * Enables or disables flash.
     *
     * @see [available]
     */
    var enable: Boolean
        /**
         * @return [Boolean.true] if flash is already on, otherwise [Boolean.false]
         */
        get() = getFlash() == CaptureResult.FLASH_MODE_TORCH
        /**
         * @param value [Boolean.true] to switch on flash, [Boolean.false] to switch off flash
         */
        set(value) {
            if (value) {
                setFlash(CaptureResult.FLASH_MODE_TORCH)
            } else {
                setFlash(CaptureResult.FLASH_MODE_OFF)
            }
        }

    private fun getFlash(): Int =
        cameraController.getSetting(CaptureRequest.FLASH_MODE) ?: CaptureResult.FLASH_MODE_OFF

    private fun setFlash(mode: Int) {
        cameraController.setSetting(CaptureRequest.FLASH_MODE, mode)
    }
}

class Exposure(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera exposure range.
     *
     * @return exposure range.
     *
     * @see [availableCompensationStep]
     * @see [compensation]
     */
    val availableCompensationRange: Range<Int>
        get() = cameraController.cameraId?.let { context.getExposureRange(it) } ?: Range(0, 0)

    /**
     * Get current camera exposure compensation step.
     *
     * This is the unit for [getExposureRange]. For example, if this key has a value of 1/2, then a
     * setting of -2 for  [getExposureRange] means that the target EV offset for the auto-exposure
     * routine is -1 EV.
     *
     * @return exposure range.
     *
     * @see [availableCompensationRange]
     * @see [compensation]
     */
    val availableCompensationStep: Rational
        get() = cameraController.cameraId?.let { context.getExposureStep(it) } ?: Rational(1, 1)

    /**
     * Set or get exposure compensation.
     *
     * @see [availableCompensationRange]
     * @see [availableCompensationStep]
     */
    var compensation: Int
        /**
         * Get the exposure compensation.
         *
         * @return exposure compensation
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ?: 0
        /**
         * Set the exposure compensation.
         *
         * @param value exposure compensation
         */
        set(value) {
            cameraController.setSetting(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                value.clamp(availableCompensationRange)
            )
        }
}


class Zoom(private val context: Context, private val cameraController: CameraController) {
    // Keep the zoomRation for Android version < R
    private var persistentZoomRatio = 1f

    /**
     * Get current camera zoom ratio range.
     *
     * @return zoom ratio range.
     *
     * @see [zoomRatio]
     */
    val availableRatioRange: Range<Float>
        get() = cameraController.cameraId?.let { context.getZoomRatioRange(it) } ?: Range(1f, 1f)

    /**
     * Set or get the current zoom ratio.
     *
     * @see [availableRatioRange]
     */
    var zoomRatio: Float
        /**
         * Get the zoom ratio.
         *
         * @return the current zoom ratio
         */
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cameraController.getSetting(CaptureRequest.CONTROL_ZOOM_RATIO) ?: 1f
        } else {
            synchronized(this) {
                persistentZoomRatio
            }
        }
        /**
         * Set the zoom ratio.
         *
         * @param value zoom ratio
         */
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraController.setSetting(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    value.clamp(availableRatioRange)
                )
            } else {
                synchronized(this) {
                    val clampedValue = value.clamp(availableRatioRange)
                    cameraController.cameraId?.let { cameraId ->
                        cameraController.setSetting(
                            CaptureRequest.SCALER_CROP_REGION,
                            getCropRegion(
                                context.getCameraCharacteristics(cameraId),
                                clampedValue
                            )
                        )
                    }
                    persistentZoomRatio = clampedValue
                }
            }
        }


    /**
     * Calculates sensor crop region for a zoom ratio (zoom >= 1.0).
     *
     * @return the crop region.
     */
    private fun getCropRegion(characteristics: CameraCharacteristics, zoomRatio: Float): Rect {
        val sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
        val xCenter: Int = sensorRect.width() / 2
        val yCenter: Int = sensorRect.height() / 2
        val xDelta = (0.5f * sensorRect.width() / zoomRatio).toInt()
        val yDelta = (0.5f * sensorRect.height() / zoomRatio).toInt()
        return Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta)
    }
}


class Focus(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera supported auto focus mode.
     *
     * @return list of supported auto focus mode
     *
     * @see [autoMode]
     */
    val availableAutoModes: List<Int>
        get() = cameraController.cameraId?.let { context.getAutoFocusModes(it) } ?: emptyList()

    /**
     * Set or get auto focus mode.
     *
     * @see [availableAutoModes]
     */
    var autoMode: Int
        /**
         * Get the auto focus mode.
         *
         * @return auto focus mode
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_AF_MODE)
            ?: CaptureResult.CONTROL_AF_MODE_OFF
        /**
         * Set the auto focus mode.
         *
         * @param value auto focus mode
         */
        set(value) {
            cameraController.setSetting(CaptureRequest.CONTROL_AF_MODE, value)
        }

    /**
     * Get current camera supported auto focus mode.
     *
     * @return list of supported auto focus mode
     *
     * @see [lensDistance]
     */
    val availableLensDistanceRange: Range<Float>
        get() = cameraController.cameraId?.let { context.getLensDistanceRange(it) } ?: Range(0F, 0f)

    /**
     * Set or get lens focus distance.
     *
     * @see [availableLensDistanceRange]
     */
    var lensDistance: Float
        /**
         * Get the lens focus distance.
         *
         * @return lens focus distance
         */
        get() = cameraController.getSetting(CaptureRequest.LENS_FOCUS_DISTANCE)
            ?: 0.0f
        /**
         * Set the lens focus distance
         *
         * Only set lens focus distance if [autoMode] == [CaptureResult.CONTROL_AF_MODE_OFF].
         *
         * @param value lens focus distance
         */
        set(value) {
            cameraController.setSetting(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                value.clamp(availableLensDistanceRange)
            )
        }
}


class Stabilization(private val context: Context, private val cameraController: CameraController) {
    /**
     * Enable or disable video stabilization.
     *
     * Do not enable both [enableVideo] and [enableOptical] at the same time.
     */
    var enableVideo: Boolean
        /**
         * Checks if video stabilization is enabled.
         *
         * @return [Boolean.true] if video stabilization is enabled, otherwise [Boolean.false]
         */
        get() = cameraController.getSetting(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) == CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
        /**
         * Enable or disable the video stabilization.
         *
         * @param value [Boolean.true] to enable video stabilization, otherwise [Boolean.false]
         */
        set(value) {
            if (value) {
                cameraController.setSetting(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
            } else {
                cameraController.setSetting(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
            }
        }

    /**
     * Check if optical video stabilization is available.
     *
     * @return [Boolean.true] if optical video stabilization is supported, otherwise [Boolean.false]
     *
     * @see [enableOptical]
     */
    val availableOptical: Boolean
        get() = cameraController.cameraId?.let { context.isOpticalStabilizationAvailable(it) }
            ?: false

    /**
     * Enable or disable optical video stabilization.
     *
     * Do not enable both [enableVideo] and [enableOptical] at the same time.
     *
     * @see [availableOptical]
     */
    var enableOptical: Boolean
        /**
         * Checks if optical video stabilization is enabled.
         *
         * @return [Boolean.true] if optical video stabilization is enabled, otherwise [Boolean.false]
         */
        get() = cameraController.getSetting(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE) == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
        /**
         * Enable or disable the optical video stabilization.
         *
         * @param value [Boolean.true] to enable optical video stabilization, otherwise [Boolean.false]
         */
        set(value) {
            if (value) {
                cameraController.setSetting(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            } else {
                cameraController.setSetting(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
        }
}
