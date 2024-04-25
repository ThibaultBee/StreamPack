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
package io.github.thibaultbee.streampack.internal.sources.video.camera

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.concurrent.Executors

/**
 * A [ICameraThreadManager] that manages camera API >= 28.
 */
class CameraExecutorManager : ICameraThreadManager {
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    @RequiresApi(Build.VERSION_CODES.P)
    @RequiresPermission(Manifest.permission.CAMERA)
    override fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    ) {
        manager.openCamera(cameraId, cameraExecutor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    ) {
        val outputConfigurations = targets.map { OutputConfiguration(it) }
        createCaptureSessionByOutputConfiguration(camera, outputConfigurations, callback)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun createCaptureSessionByOutputConfiguration(
        camera: CameraDevice,
        outputConfigurations: List<OutputConfiguration>,
        callback: CameraCaptureSession.StateCallback
    ) {
        SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputConfigurations,
            cameraExecutor,
            callback
        ).also { sessionConfig ->
            camera.createCaptureSession(sessionConfig)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setRepeatingSingleRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.setSingleRepeatingRequest(captureRequest, cameraExecutor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun captureBurstRequests(
        captureSession: CameraCaptureSession,
        captureRequests: List<CaptureRequest>,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.captureBurstRequests(captureRequests, cameraExecutor, callback)
    }

    override fun release() {
    }
}