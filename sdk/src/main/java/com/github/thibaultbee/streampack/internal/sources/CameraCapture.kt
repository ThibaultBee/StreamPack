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
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.error.CameraError
import com.github.thibaultbee.streampack.internal.events.EventHandler
import com.github.thibaultbee.streampack.internal.interfaces.Controllable
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.*
import java.security.InvalidParameterException
import java.util.concurrent.Executors


class CameraCapture(
    private val context: Context,
    override val onInternalErrorListener: OnErrorListener,
    val logger: ILogger
) : EventHandler(), Controllable {
    var fpsRange = Range(0, 30)

    var previewSurface: Surface? = null
    var encoderSurface: Surface? = null

    var cameraId: String = "0"
    var isStreaming = false
    private var restartStream = false

    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.e(this, "Camera Session configuration failed")
            session.close()
            reportError(CameraError("Camera: failed to configure the capture session"))
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            captureRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder?.let { captureRequest ->
                previewSurface?.let { surface -> captureRequest.addTarget(surface) }
                if (restartStream) {
                    encoderSurface?.let { surface -> captureRequest.addTarget(surface) }
                }
                captureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                captureSession?.setRepeatingRequest(
                    captureRequest.build(),
                    null,
                    null
                )
            }
        }

        override fun onClosed(session: CameraCaptureSession) {
            captureRequestBuilder = null
            captureSession = null
        }
    }

    private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@CameraCapture.camera = camera
            cameraId = camera.id
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            logger.e(this, "Camera ${camera.id} is in error $error")
            reportError(
                when (error) {
                    ERROR_CAMERA_IN_USE -> CameraError("Camera already in use")
                    ERROR_MAX_CAMERAS_IN_USE -> CameraError("Max cameras in use")
                    ERROR_CAMERA_DISABLED -> CameraError("Camera has been disabled")
                    ERROR_CAMERA_DEVICE -> CameraError("Camera device has crashed")
                    ERROR_CAMERA_SERVICE -> CameraError("Camera service has crashed")
                    else -> CameraError("Unknown error")
                }
            )
            this@CameraCapture.camera = null
        }

        override fun onClosed(camera: CameraDevice) {
            this@CameraCapture.camera = null
            logger.d(this, "Camera ${camera.id} is closed")
        }
    }

    // Before Android 28
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // After Android 28
    private var executor = Executors.newSingleThreadExecutor()

    private fun createCaptureSession() {
        val surfaceList = mutableListOf<Surface>()
        previewSurface?.let { surfaceList.add(it) }
        encoderSurface?.let { surfaceList.add(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val outputs = mutableListOf<OutputConfiguration>()
            surfaceList.forEach { outputs.add(OutputConfiguration(it)) }
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                executor,
                captureSessionCallback
            ).also { sessionConfig ->
                camera?.createCaptureSession(sessionConfig)
            }
        } else {
            @Suppress("deprecation")
            camera?.createCaptureSession(surfaceList, captureSessionCallback, backgroundHandler)
        }
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
    fun startPreview(cameraId: String, restartStream: Boolean = false) {
        this.restartStream = restartStream
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraManager.openCamera(cameraId, executor, cameraDeviceCallback)
        } else {
            startBackgroundThread()
            cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler!!)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview() {
        startPreview(cameraId)
    }

    fun stopPreview() {
        captureRequestBuilder = null

        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null

        stopBackgroundThread()
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

    override fun startStream() {
        captureRequestBuilder?.let {
            it.addTarget(encoderSurface!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                captureSession?.setSingleRepeatingRequest(
                    it.build(),
                    executor,
                    captureCallback
                )
            } else {
                captureSession?.setRepeatingRequest(
                    it.build(),
                    captureCallback,
                    backgroundHandler
                )
            }
        } ?: throw IllegalStateException("Camera is not ready for stream")
        isStreaming = true
    }

    override fun stopStream() {
        isStreaming = false
        captureRequestBuilder?.let {
            it.removeTarget(encoderSurface!!)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                backgroundHandler
            )
        }
    }

    override fun release() {
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("BackgroundCamera").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}