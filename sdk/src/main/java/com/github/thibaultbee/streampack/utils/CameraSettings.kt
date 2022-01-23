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