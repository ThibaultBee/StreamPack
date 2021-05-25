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
package com.github.thibaultbee.streampack.internal.sources

import android.Manifest
import android.content.Context
import android.hardware.camera2.*
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.error.CameraError
import com.github.thibaultbee.streampack.internal.events.EventHandler
import com.github.thibaultbee.streampack.internal.interfaces.Controllable
import com.github.thibaultbee.streampack.internal.sources.camera.CameraExecutorManager
import com.github.thibaultbee.streampack.internal.sources.camera.CameraHandlerManager
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.ILogger
import com.github.thibaultbee.streampack.utils.getCameraFpsList
import kotlinx.coroutines.*
import java.security.InvalidParameterException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraCapture(
    private val context: Context,
    override val onInternalErrorListener: OnErrorListener,
    val logger: ILogger
) : EventHandler(), Controllable {
    var previewSurface: Surface? = null
    var encoderSurface: Surface? = null
    var cameraId: String
        get() = camera?.id ?: "0"
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            val restartStream = isStreaming
            stopPreview()
            startPreviewAsync(value, restartStream)
        }

    private var fpsRange = Range(0, 30)
    private var isStreaming = false

    private val scope = CoroutineScope(Dispatchers.IO)

    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private val threadManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        CameraExecutorManager()
    } else {
        CameraHandlerManager()
    }

    private fun getClosestFpsRange(fps: Int): Range<Int> {
        var fpsRangeList = context.getCameraFpsList(cameraId)
        // Get range that contains FPS
        fpsRangeList =
            fpsRangeList.filter { it.contains(fps) or it.contains(fps * 1000) } // On Samsung S4 fps range is [4000-30000] instead of [4-30]
        if (fpsRangeList.isEmpty()) {
            throw InvalidParameterException("Failed to find a single FPS range that contains $fps")
        }

        // Get smaller range
        var selectedFpsRange = fpsRangeList[0]
        fpsRangeList = fpsRangeList.drop(0)
        fpsRangeList.forEach {
            if ((it.upper - it.lower) < (selectedFpsRange.upper - selectedFpsRange.lower)) {
                selectedFpsRange = it
            }
        }

        return selectedFpsRange
    }

    fun configure(fps: Int) {
        fpsRange = getClosestFpsRange(fps)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        threadManager.openCamera(manager, cameraId, CameraDeviceCallback(cont, logger))
    }

    private suspend fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        threadManager.createCaptureSession(
            camera,
            targets,
            CameraCaptureSessionCallback(cont, logger)
        )
    }

    private fun createRequestSession(
        camera: CameraDevice,
        captureSession: CameraCaptureSession,
        surfaces: List<Surface>
    ) {
        if (surfaces.isEmpty()) {
            logger.w(this, "No target surface set")
            return
        }

        camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            surfaces.forEach { addTarget(it) }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            threadManager.setRepeatingRequest(captureSession, build(), captureCallback)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun startPreview(cameraId: String, restartStream: Boolean = false) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camera = openCamera(manager, cameraId)

        var targets = mutableListOf<Surface>()
        previewSurface?.let { targets.add(it) }
        encoderSurface?.let { targets.add(it) }

        camera?.let {
            captureSession = createCaptureSession(it, targets)

            targets = mutableListOf()
            previewSurface?.let { targets.add(it) }
            if (restartStream) {
                encoderSurface?.let { targets.add(it) }
            }
            createRequestSession(it, captureSession!!, targets)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreviewAsync(cameraId: String, restartStream: Boolean = false) = scope.async {
        startPreview(cameraId, restartStream)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreviewAsync() = startPreviewAsync(cameraId)

    fun stopPreview() {
        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null
    }

    override fun startStream() {
        if ((camera != null) && (captureSession != null)) {
            val targets = mutableListOf<Surface>()
            previewSurface?.let { targets.add(it) }
            encoderSurface?.let { targets.add(it) }
            createRequestSession(camera!!, captureSession!!, targets)
            isStreaming = true
        } else {
            throw IllegalStateException("Camera is not ready for stream")
        }
    }

    override fun stopStream() {
        isStreaming = false
        if ((camera != null) && (captureSession != null)) {
            val targets = mutableListOf<Surface>()
            previewSurface?.let { targets.add(it) }
            createRequestSession(camera!!, captureSession!!, targets)
        }
    }

    override fun release() {
        threadManager.release()
    }

    private class CameraDeviceCallback(
        private val cont: CancellableContinuation<CameraDevice>,
        private val logger: ILogger
    ) : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) = cont.resume(device)

        override fun onDisconnected(camera: CameraDevice) {
            logger.w(this, "Camera ${camera.id} has been disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            logger.e(this, "Camera ${camera.id} is in error $error")

            val exc = when (error) {
                ERROR_CAMERA_IN_USE -> CameraError("Camera already in use")
                ERROR_MAX_CAMERAS_IN_USE -> CameraError("Max cameras in use")
                ERROR_CAMERA_DISABLED -> CameraError("Camera has been disabled")
                ERROR_CAMERA_DEVICE -> CameraError("Camera device has crashed")
                ERROR_CAMERA_SERVICE -> CameraError("Camera service has crashed")
                else -> CameraError("Unknown error")
            }
            if (cont.isActive) cont.resumeWithException(exc)
        }
    }

    private class CameraCaptureSessionCallback(
        private val cont: CancellableContinuation<CameraCaptureSession>,
        private val logger: ILogger
    ) : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.e(this, "Camera Session configuration failed")
            cont.resumeWithException(CameraError("Camera: failed to configure the capture session"))
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            logger.e(this, "Capture failed  with code ${failure.reason}")
        }
    }
}