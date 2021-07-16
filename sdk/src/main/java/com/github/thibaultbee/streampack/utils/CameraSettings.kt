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

import android.hardware.camera2.CaptureResult
import com.github.thibaultbee.streampack.internal.sources.camera.CameraController
import com.github.thibaultbee.streampack.streamers.BaseCameraStreamer

/**
 * Use to change camera settings.
 * This object is returned by [BaseCameraStreamer.cameraSettings].
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