package com.github.thibaultbee.streampack.utils

import android.hardware.camera2.CaptureResult
import com.github.thibaultbee.streampack.internal.sources.camera.CameraController

/**
 * Use to change camera settings.
 */
class CameraSettings(private val cameraController: CameraController) {
    /**
     * Enables or disables flash
     */
    var flashEnable: Boolean
        /**
         * @return [Boolean.true] if flash is already on, otherwise [Boolean.false]
         */
        get() = cameraController.getFlash() == CaptureResult.FLASH_MODE_TORCH
        /**
         * @param value [Boolean.true] to switch on flash, [Boolean.false] to switch off flash
         */
        set(value) {
            if (value) {
                cameraController.setFlash(CaptureResult.FLASH_MODE_TORCH)
            } else {
                cameraController.setFlash(CaptureResult.FLASH_MODE_OFF)
            }
        }
}