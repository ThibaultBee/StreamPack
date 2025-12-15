/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraException
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraDeviceController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.targetFps
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal object CameraUtils {
    private const val TAG = "CameraUtils"

    @RequiresPermission(Manifest.permission.CAMERA)
    internal suspend fun openCamera(
        sessionCompat: ICameraCaptureSessionCompat,
        manager: CameraManager,
        cameraId: String,
        isClosedFlow: MutableStateFlow<Boolean>,
        throwableFlow: MutableStateFlow<Throwable?>
    ): CameraDevice = suspendCoroutine { continuation ->
        val callbacks = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resume(device)

            override fun onDisconnected(camera: CameraDevice) {
                try {
                    continuation.resumeWithException(RuntimeException("Camera has been disconnected"))
                } catch (_: IllegalStateException) {
                    // Ignore if the continuation has already been resumed
                }
                Logger.w(TAG, "Camera ${camera.id} has been disconnected")
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Logger.e(TAG, "Camera ${camera.id} is in error $error")

                val exception = when (error) {
                    ERROR_CAMERA_IN_USE -> CameraException("Camera already in use")
                    ERROR_MAX_CAMERAS_IN_USE -> CameraException("Max cameras in use")
                    ERROR_CAMERA_DISABLED -> CameraException("Camera has been disabled")
                    ERROR_CAMERA_DEVICE -> CameraException("Camera device has crashed")
                    ERROR_CAMERA_SERVICE -> CameraException("Camera service has crashed")
                    else -> CameraException("Unknown error")
                }
                try {
                    continuation.resumeWithException(exception)
                } catch (_: IllegalStateException) {
                    // Ignore if the continuation has already been resumed
                }
                camera.close()
                throwableFlow.tryEmit(exception)
            }

            override fun onClosed(camera: CameraDevice) {
                isClosedFlow.tryEmit(true)
                Logger.i(TAG, "Camera ${camera.id} has been closed")
            }
        }
        try {
            sessionCompat.openCamera(manager, cameraId, callbacks)
        } catch (_: Throwable) {
            isClosedFlow.tryEmit(true)
        }
    }

    internal suspend fun createCaptureSession(
        cameraDeviceController: CameraDeviceController,
        outputs: List<Surface>,
        dynamicRange: Long,
        isClosedFlow: MutableStateFlow<Boolean>
    ): CameraCaptureSession = suspendCoroutine { continuation ->
        val cameraId = cameraDeviceController.id
        val callbacks = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Logger.i(
                    TAG,
                    "Camera session configured for camera $cameraId and outputs $outputs"
                )
                continuation.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                isClosedFlow.tryEmit(true)
                Logger.e(
                    TAG,
                    "Camera session configuration failed for camera $cameraId and outputs $outputs"
                )
                try {
                    continuation.resumeWithException(CameraException("Camera: failed to configure the capture session for camera $cameraId and outputs $outputs"))
                } catch (_: IllegalStateException) {
                    // Ignore if the continuation has already been resumed
                }
            }

            override fun onClosed(session: CameraCaptureSession) {
                isClosedFlow.tryEmit(true)
                Logger.e(
                    TAG,
                    "Camera capture session closed for camera $cameraId and outputs $outputs"
                )
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val outputConfigurations = outputs.map {
                    OutputConfiguration(it).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            dynamicRangeProfile = dynamicRange
                        }
                    }
                }

                cameraDeviceController.createCaptureSessionByOutputConfiguration(
                    outputConfigurations, callbacks
                )
            } else {
                cameraDeviceController.createCaptureSession(
                    outputs, callbacks
                )
            }
        } catch (_: Throwable) {
            isClosedFlow.tryEmit(true)
        }
    }

    internal fun getClosestFpsRange(
        characteristics: CameraCharacteristics,
        fps: Int
    ): Range<Int> {
        var fpsRangeList = characteristics.targetFps
        Logger.i(TAG, "Supported FPS range list: $fpsRangeList")

        // Get range that contains FPS
        fpsRangeList =
            fpsRangeList.filter { it.contains(fps) or it.contains(fps * 1000) } // On Samsung S4 fps range is [4000-30000] instead of [4-30]
        if (fpsRangeList.isEmpty()) {
            Logger.w(
                TAG,
                "Failed to find a single FPS range that contains $fps. Trying with forced $fps."
            )
            return Range(fps, fps)
        }

        // Get smaller range
        var selectedFpsRange = fpsRangeList[0]
        fpsRangeList = fpsRangeList.drop(0)
        fpsRangeList.forEach {
            if ((it.upper - it.lower) < (selectedFpsRange.upper - selectedFpsRange.lower)) {
                selectedFpsRange = it
            }
        }

        Logger.d(TAG, "Selected Fps range $selectedFpsRange")
        return selectedFpsRange
    }
}