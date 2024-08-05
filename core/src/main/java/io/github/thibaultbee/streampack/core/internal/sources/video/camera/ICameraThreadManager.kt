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
package io.github.thibaultbee.streampack.core.internal.sources.video.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.view.Surface

/**
 * Encapsulates camera2 API changes and deprecation.
 */
interface ICameraThreadManager {

    /**
     * Opens camera device.
     *
     * @param manager the [CameraManager]
     * @param cameraId the camera id
     * @param callback an implementation of [CameraDevice.StateCallback]
     */
    fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    )

    /**
     * Create a camera capture session for surfaces.
     *
     * @param camera the [CameraDevice]
     * @param targets list of [Surface]
     * @param callback an implementation of [CameraCaptureSession.StateCallback]
     */
    fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    )

    /**
     * Create a camera capture session for output configurations.
     *
     * @param camera the [CameraDevice]
     * @param outputConfigurations list of [OutputConfiguration]
     * @param callback an implementation of [CameraCaptureSession.StateCallback]
     */
    fun createCaptureSessionByOutputConfiguration(
        camera: CameraDevice,
        outputConfigurations: List<OutputConfiguration>,
        callback: CameraCaptureSession.StateCallback
    )

    /**
     * Set a repeating request.
     *
     * @param captureSession the [CameraCaptureSession]
     * @param captureRequest the [CaptureRequest]
     * @param callback an implementation of [CameraCaptureSession.CaptureCallback]
     * @return A unique capture sequence ID
     */
    fun setRepeatingSingleRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ): Int

    /**
     * Capture multiple burst requests.
     *
     * @param captureSession the [CameraCaptureSession]
     * @param captureRequests the list of [CaptureRequest]
     * @param callback an implementation of [CameraCaptureSession.CaptureCallback]
     * @return A unique capture sequence ID
     */
    fun captureBurstRequests(
        captureSession: CameraCaptureSession,
        captureRequests: List<CaptureRequest>,
        callback: CameraCaptureSession.CaptureCallback
    ): Int

    /**
     * Release internal object.
     */
    fun release()
}