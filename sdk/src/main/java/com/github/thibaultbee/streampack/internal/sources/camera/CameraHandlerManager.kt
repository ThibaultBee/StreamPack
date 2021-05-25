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
package com.github.thibaultbee.streampack.internal.sources.camera

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.RequiresPermission

/**
 * As camera API that support a [Handler] are deprecated since API >= 30.
 * Is is a [ICameraThreadManager] that manages camera API < 30.
 */
class CameraHandlerManager : ICameraThreadManager {
    private var cameraThread = HandlerThread("CameraThread").apply { start() }
    private var cameraHandler = Handler(cameraThread.looper)

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    ) {
        manager.openCamera(cameraId, callback, cameraHandler)
    }

    override fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    ) {
        @Suppress("deprecation")
        camera.createCaptureSession(targets, callback, cameraHandler)
    }

    override fun setRepeatingRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ) {
        captureSession.setRepeatingRequest(captureRequest, callback, cameraHandler)
    }

    override fun release() {
        cameraThread.quitSafely()
        try {
            cameraThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


}