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

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.AUDIO_RESTRICTION_NONE
import android.hardware.camera2.CameraDevice.AUDIO_RESTRICTION_VIBRATION_SOUND
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.error.CameraError
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.getCameraFps
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraController(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var camera: CameraDevice? = null
    val cameraId: String?
        get() = camera?.id

    private var captureSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest.Builder? = null

    /**
     * List of surfaces used in the current request session.
     */
    private val requestSessionSurface = mutableSetOf<Surface>()

    private val cameraDispatchManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        CameraExecutorManager()
    } else {
        CameraHandlerManager()
    }

    private fun getClosestFpsRange(cameraId: String, fps: Int): Range<Int> {
        var fpsRangeList = context.getCameraFps(cameraId)
        Logger.i(TAG, "Supported FPS range list: $fpsRangeList")

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

        Logger.d(TAG, "Selected Fps range $selectedFpsRange")
        return selectedFpsRange
    }

    private class CameraDeviceCallback(
        private val cont: CancellableContinuation<CameraDevice>,
    ) : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) = cont.resume(device)

        override fun onDisconnected(camera: CameraDevice) {
            Logger.w(TAG, "Camera ${camera.id} has been disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Logger.e(TAG, "Camera ${camera.id} is in error $error")

            val exc = when (error) {
                ERROR_CAMERA_IN_USE -> CameraError("Camera already in use")
                ERROR_MAX_CAMERAS_IN_USE -> CameraError(
                    "Max cameras in use"
                )

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
    ) : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Logger.e(TAG, "Camera Session configuration failed")
            cont.resumeWithException(CameraError("Camera: failed to configure the capture session"))
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Logger.e(TAG, "Capture failed with code ${failure.reason}")
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun openCamera(
        manager: CameraManager, cameraId: String
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        cameraDispatchManager.openCamera(
            manager, cameraId, CameraDeviceCallback(cont)
        )
    }

    private suspend fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        dynamicRange: Long,
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val outputConfigurations = targets.map {
                OutputConfiguration(it).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        dynamicRangeProfile = dynamicRange
                    }
                }
            }

            cameraDispatchManager.createCaptureSessionByOutputConfiguration(
                camera, outputConfigurations, CameraCaptureSessionCallback(cont)
            )
        } else {
            cameraDispatchManager.createCaptureSession(
                camera, targets, CameraCaptureSessionCallback(cont)
            )
        }
    }

    private fun createRequestSession(
        camera: CameraDevice,
        captureSession: CameraCaptureSession,
        fpsRange: Range<Int>,
        surfaces: List<Surface>
    ): CaptureRequest.Builder {
        return camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            surfaces.forEach { addTarget(it) }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            cameraDispatchManager.setRepeatingSingleRequest(
                captureSession,
                build(),
                captureCallback
            )
        }
    }

    val isCameraRunning: Boolean
        get() = camera != null

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startCamera(
        cameraId: String,
        targets: List<Surface>,
        dynamicRange: Long,
    ) {
        require(targets.isNotEmpty()) { " At least one target is required" }

        withContext(coroutineDispatcher) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            camera = openCamera(manager, cameraId).also { cameraDevice ->
                captureSession = createCaptureSession(
                    cameraDevice, targets, dynamicRange
                )
            }
        }
    }

    val isRequestSessionRunning: Boolean
        get() = captureRequest != null

    fun startRequestSession(fps: Int, targets: List<Surface>) {
        require(camera != null) { "Camera must not be null" }
        require(captureSession != null) { "Capture session must not be null" }

        captureRequest = createRequestSession(
            camera!!, captureSession!!, getClosestFpsRange(camera!!.id, fps), targets
        )
        requestSessionSurface.addAll(targets)
    }

    fun stop() {
        requestSessionSurface.clear()
        captureRequest = null

        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null
    }

    /**
     * Whether the target is in the current request session.
     */
    fun hasTarget(target: Surface): Boolean {
        return requestSessionSurface.contains(target)
    }

    fun addTargets(targets: List<Surface>) {
        require(captureRequest != null) { "capture request must not be null" }
        require(targets.isNotEmpty()) { " At least one target is required" }

        targets.forEach {
            if (!hasTarget(it)) {
                captureRequest!!.addTarget(it)
                requestSessionSurface.add(it)
            }
        }
        updateRepeatingSession()
    }

    fun addTarget(target: Surface) {
        require(captureRequest != null) { "capture request must not be null" }

        if (hasTarget(target)) {
            return
        }

        captureRequest!!.addTarget(target)
        requestSessionSurface.add(target)

        updateRepeatingSession()
    }

    fun removeTargets(targets: List<Surface>) {
        require(captureRequest != null) { "capture request must not be null" }

        targets.forEach {
            captureRequest!!.removeTarget(it)
            requestSessionSurface.remove(it)
        }

        if (requestSessionSurface.isNotEmpty()) {
            updateRepeatingSession()
        } else {
            stop()
        }
    }

    fun removeTarget(target: Surface) {
        require(captureRequest != null) { "capture request must not be null" }

        captureRequest!!.removeTarget(target)
        requestSessionSurface.remove(target)

        if (requestSessionSurface.isNotEmpty()) {
            updateRepeatingSession()
        } else {
            stop()
        }
    }

    fun release() {
        cameraDispatchManager.release()
    }

    fun muteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                camera?.cameraAudioRestriction = AUDIO_RESTRICTION_VIBRATION_SOUND
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to mute vibration and sound", t)
            }
        }
    }

    fun unmuteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                camera?.cameraAudioRestriction = AUDIO_RESTRICTION_NONE
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to unmute vibration and sound", t)
            }
        }
    }

    fun updateRepeatingSession() {
        require(captureSession != null) { "capture session must not be null" }
        require(captureRequest != null) { "capture request must not be null" }

        cameraDispatchManager.setRepeatingSingleRequest(
            captureSession!!, captureRequest!!.build(), captureCallback
        )
    }

    private fun updateBurstSession() {
        require(captureSession != null) { "capture session must not be null" }
        require(captureRequest != null) { "capture request must not be null" }

        cameraDispatchManager.captureBurstRequests(
            captureSession!!, listOf(captureRequest!!.build()), captureCallback
        )
    }

    fun <T> getSetting(key: CaptureRequest.Key<T>?): T? {
        return captureRequest?.get(key)
    }

    fun <T> setRepeatingSetting(key: CaptureRequest.Key<T>, value: T) {
        captureRequest?.let {
            it.set(key, value)
            updateRepeatingSession()
        }
    }

    fun setRepeatingSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        captureRequest?.let {
            for (item in settingsMap) {
                it.set(item.key, item.value)
            }
            updateRepeatingSession()
        }
    }

    fun setBurstSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        captureRequest?.let {
            for (item in settingsMap) {
                it.set(item.key, item.value)
            }
            updateBurstSession()
        }
    }

    companion object {
        private const val TAG = "CameraController"
    }
}