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
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Rational
import com.github.thibaultbee.streampack.internal.sources.camera.CameraController
import com.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer

/**
 * Use to change camera settings.
 * This object is returned by [BaseCameraStreamer.cameraSettings].
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
            cameraController.setSetting(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value)
        }
}


class Zoom(private val context: Context, private val cameraController: CameraController) {
    /**
     * Get current camera zoom ratio range.
     *
     * Only for Android version >= R (30)
     *
     * @return zoom ratio range.
     *
     * @see [zoomRatio]
     */
    val availableRatioRange: Range<Float>
        get() = cameraController.cameraId?.let { context.getZoomRatioRange(it) } ?: Range(1f, 1f)

    /**
     * Set or get exposure compensation.
     *
     * @see [availableRatioRange]
     */
    var zoomRatio: Float
        /**
         * Get the exposure compensation.
         *
         * @return exposure compensation
         */
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cameraController.getSetting(CaptureRequest.CONTROL_ZOOM_RATIO) ?: 1f
        } else {
            1f
        }
        /**
         * Set the exposure compensation.
         *
         * @param value exposure compensation
         */
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraController.setSetting(CaptureRequest.CONTROL_ZOOM_RATIO, value)
            }
        }
}