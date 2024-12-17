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
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.dispatchers.CameraDispatchers
import io.github.thibaultbee.streampack.core.internal.utils.extensions.resumeIfActive
import io.github.thibaultbee.streampack.core.internal.utils.extensions.resumeWithExceptionIfActive
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.extensions.getAutoFocusModes
import io.github.thibaultbee.streampack.core.utils.extensions.getCameraFps
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

    /**
     * Camera dispatcher to use. Either run on executor or handler.
     */
    private val cameraDispatcher = CameraDispatchers.build()

    private fun getClosestFpsRange(cameraId: String, fps: Int): Range<Int> {
        var fpsRangeList = context.getCameraFps(cameraId)
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

    private class CameraDeviceCallback(
        private val continuation: CancellableContinuation<CameraDevice>,
    ) : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) = continuation.resume(device)

        override fun onDisconnected(camera: CameraDevice) {
            Logger.w(TAG, "Camera ${camera.id} has been disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Logger.e(TAG, "Camera ${camera.id} is in error $error")

            val exc = when (error) {
                ERROR_CAMERA_IN_USE -> CameraException("Camera already in use")
                ERROR_MAX_CAMERAS_IN_USE -> CameraException(
                    "Max cameras in use"
                )

                ERROR_CAMERA_DISABLED -> CameraException("Camera has been disabled")
                ERROR_CAMERA_DEVICE -> CameraException("Camera device has crashed")
                ERROR_CAMERA_SERVICE -> CameraException("Camera service has crashed")
                else -> CameraException("Unknown error")
            }
            if (continuation.isActive) continuation.resumeWithException(exc)
        }
    }

    private class CameraCaptureSessionCallback(
        private val continuation: CancellableContinuation<CameraCaptureSession>,
    ) : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) = continuation.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Logger.e(TAG, "Camera Session configuration failed")
            continuation.resumeWithException(CameraException("Camera: failed to configure the capture session"))
        }

        override fun onClosed(session: CameraCaptureSession) {
            Logger.e(TAG, "Camera Session configuration closed")
        }
    }

    private class CameraCaptureSessionCaptureCallback(
        private val onCaptureRequestComparator: (CaptureRequest) -> Boolean,
        private val continuation: CancellableContinuation<Unit>,
    ) : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
        ) {
            if (onCaptureRequestComparator(request)) {
                continuation.resumeIfActive(Unit)
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
        ) {
            if (onCaptureRequestComparator(request)) {
                Logger.e(TAG, "Capture failed with code ${failure.reason}")
                continuation.resumeWithExceptionIfActive(CameraException("Capture failed with code ${failure.reason}"))
            }
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
        cameraDispatcher.openCamera(
            manager, cameraId, CameraDeviceCallback(cont)
        )
    }

    private suspend fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        dynamicRange: Long,
    ): CameraCaptureSession = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val outputConfigurations = targets.map {
                OutputConfiguration(it).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        dynamicRangeProfile = dynamicRange
                    }
                }
            }

            cameraDispatcher.createCaptureSessionByOutputConfiguration(
                camera, outputConfigurations, CameraCaptureSessionCallback(continuation)
            )
        } else {
            cameraDispatcher.createCaptureSession(
                camera, targets, CameraCaptureSessionCallback(continuation)
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
            if (context.getAutoFocusModes(camera.id)
                    .contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            ) {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }
            cameraDispatcher.setRepeatingSingleRequest(
                captureSession, build(), captureCallback
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
        val camera = requireNotNull(camera) { "Camera must not be null" }
        val captureSession = requireNotNull(captureSession) { "Capture session must not be null" }

        captureRequest = createRequestSession(
            camera, captureSession, getClosestFpsRange(camera.id, fps), targets
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
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }
        require(targets.isNotEmpty()) { " At least one target is required" }

        targets.forEach {
            if (!hasTarget(it)) {
                captureRequest.addTarget(it)
                requestSessionSurface.add(it)
            }
        }
        updateRepeatingSession()
    }

    fun addTarget(target: Surface) {
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        if (hasTarget(target)) {
            return
        }

        captureRequest.addTarget(target)
        requestSessionSurface.add(target)

        updateRepeatingSession()
    }

    suspend fun removeTargets(targets: List<Surface>) {
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        targets.forEach {
            captureRequest.removeTarget(it)
            requestSessionSurface.remove(it)
        }

        if (requestSessionSurface.isNotEmpty()) {
            val tag = "removeTargets-${System.currentTimeMillis()}"
            captureRequest.setTag(tag)
            withContext(coroutineDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    updateRepeatingSession(
                        CameraCaptureSessionCaptureCallback(
                            { it.tag == tag }, continuation
                        )
                    )
                }
            }
        } else {
            stop()
        }
    }

    suspend fun removeTarget(target: Surface) {
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        captureRequest.removeTarget(target)
        requestSessionSurface.remove(target)

        if (requestSessionSurface.isNotEmpty()) {
            val tag = "removeTarget-${System.currentTimeMillis()}"
            captureRequest.setTag(tag)
            withContext(coroutineDispatcher) {
                suspendCancellableCoroutine { continuation ->
                    updateRepeatingSession(
                        CameraCaptureSessionCaptureCallback(
                            { it.tag == tag }, continuation
                        )
                    )
                }
            }
        } else {
            stop()
        }
    }

    fun release() {
        stop()
        cameraDispatcher.release()
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

    fun updateRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val captureSession = requireNotNull(captureSession) { "capture session must not be null" }
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        cameraDispatcher.setRepeatingSingleRequest(
            captureSession, captureRequest.build(), cameraCaptureCallback
        )
    }

    private fun updateBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback) {
        val captureSession = requireNotNull(captureSession) { "capture session must not be null" }
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        cameraDispatcher.captureBurstRequests(
            captureSession, listOf(captureRequest.build()), cameraCaptureCallback
        )
    }

    fun <T> getSetting(key: CaptureRequest.Key<T?>): T? {
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
            updateBurstSession(captureCallback)
        }
    }

    companion object {
        private const val TAG = "CameraController"
    }
}