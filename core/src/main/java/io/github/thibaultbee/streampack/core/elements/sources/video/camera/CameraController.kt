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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera

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
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.dispatchers.CameraDispatchers
import io.github.thibaultbee.streampack.core.elements.utils.extensions.resumeIfActive
import io.github.thibaultbee.streampack.core.elements.utils.extensions.resumeWithExceptionIfActive
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CameraController(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * Mutex to avoid concurrent access to camera.
     */
    private val cameraMutex = Mutex()

    private var camera: CameraDevice? = null
    val cameraId: String?
        get() = camera?.id

    private var captureSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest.Builder? = null

    /**
     * List of surfaces used in the current capture session.
     */
    private val captureSessionSurface = mutableSetOf<Surface>()

    /**
     * List of surfaces used in the current request session.
     */
    private val requestSessionSurface = mutableSetOf<Surface>()

    /**
     * Camera dispatcher to use. Either run on executor or handler.
     */
    private val cameraDispatcher = CameraDispatchers.build()

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

    suspend fun isCameraRunning(): Boolean = executeSafely {
        camera != null
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startCamera(
        cameraId: String,
    ) {
        executeSafely {
            camera = CameraUtils.openCamera(cameraDispatcher, cameraManager, cameraId)
        }
    }

    suspend fun stopCamera() {
        executeSafely {
            stopCaptureSessionSync()

            camera?.close()
            camera = null
        }
    }

    /**
     * Whether the target is in the current capture session.
     */
    fun isSurfaceRegistered(target: Surface): Boolean {
        return captureSessionSurface.contains(target)
    }

    suspend fun isCaptureSessionRunning(): Boolean = executeSafely {
        captureSession != null
    }

    suspend fun startCaptureSession(targets: List<Surface>, dynamicRange: Long) {
        requireNotNull(camera) { "Camera must not be null" }
        require(targets.isNotEmpty()) { " At least one target is required" }

        executeSafely {
            val camera = camera ?: run {
                Logger.i(TAG, "Camera has been closed")
                return@executeSafely
            }
            captureSession = CameraUtils.createCaptureSession(
                cameraDispatcher, camera, targets, dynamicRange
            )
            captureSessionSurface.addAll(targets)
        }
    }

    private fun stopCaptureSessionSync() {
        requestSessionSurface.clear()
        captureRequest = null

        captureSessionSurface.clear()
        captureSession?.close()
        captureSession = null
    }

    suspend fun stopCaptureSession() {
        executeSafely {
            stopCaptureSessionSync()
        }
    }

    suspend fun isRequestSessionRunning(): Boolean = executeSafely {
        captureRequest != null
    }

    fun isSurfaceRunning(surface: Surface): Boolean {
        return requestSessionSurface.contains(surface)
    }

    suspend fun startRequestSession(fps: Int, targets: List<Surface>) {
        requireNotNull(camera) { "Camera must not be null" }
        requireNotNull(captureSession) { "Capture session must not be null" }

        executeSafely {
            val camera = camera ?: run {
                Logger.i(TAG, "Camera has been closed")
                return@executeSafely
            }
            val captureSession = captureSession ?: run {
                Logger.i(TAG, "Capture session has been closed")
                return@executeSafely
            }
            captureRequest = CameraUtils.createRequestSession(
                cameraDispatcher,
                context,
                camera,
                captureSession,
                CameraUtils.getClosestFpsRange(context, camera.id, fps),
                targets
            )
            requestSessionSurface.addAll(targets)
        }
    }

    suspend fun addTargets(targets: List<Surface>) {
        require(targets.isNotEmpty()) { " At least one target is required" }
        require(targets.all { captureSessionSurface.contains(it) }) { "Targets must be in the current capture session" }
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        if (targets.all { isSurfaceRunning(it) }) {
            return
        }

        executeSafely {
            targets.forEach {
                // Adding a target more than once has no effect.
                captureRequest.addTarget(it)
                requestSessionSurface.addAll(targets)
            }
            setRepeatingSession()
        }
    }

    suspend fun addTarget(target: Surface) {
        require(captureSessionSurface.contains(target)) { "Target must be in the current capture session" }
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        if (isSurfaceRunning(target)) {
            return
        }

        executeSafely {
            // Adding a target more than once has no effect.
            captureRequest.addTarget(target)
            requestSessionSurface.add(target)

            setRepeatingSession()
        }
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
            executeSafely {
                suspendCancellableCoroutine { continuation ->
                    setRepeatingSession(
                        CameraCaptureSessionCaptureCallback(
                            { it.tag == tag }, continuation
                        )
                    )
                }
            }
        } else {
            stopCaptureSession()
        }
    }

    suspend fun removeTarget(target: Surface) {
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        captureRequest.removeTarget(target)
        requestSessionSurface.remove(target)

        if (requestSessionSurface.isNotEmpty()) {
            val tag = "removeTarget-${System.currentTimeMillis()}"
            captureRequest.setTag(tag)
            executeSafely {
                suspendCancellableCoroutine { continuation ->
                    setRepeatingSession(
                        CameraCaptureSessionCaptureCallback(
                            { it.tag == tag }, continuation
                        )
                    )
                }
            }
        } else {
            stopCaptureSession()
        }
    }

    fun release() {
        runBlocking {
            stopCamera()
        }
        cameraDispatcher.release()
    }

    private suspend fun <T> executeSafely(block: suspend () -> T) =
        withContext(coroutineDispatcher) {
            cameraMutex.withLock {
                block()
            }
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

    fun setRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val captureSession = requireNotNull(captureSession) { "capture session must not be null" }
        val captureRequest = requireNotNull(captureRequest) { "capture request must not be null" }

        cameraDispatcher.setRepeatingSingleRequest(
            captureSession, captureRequest.build(), cameraCaptureCallback
        )
    }

    private fun setBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback) {
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
            setRepeatingSession()
        }
    }

    fun setRepeatingSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        captureRequest?.let {
            for (item in settingsMap) {
                it.set(item.key, item.value)
            }
            setRepeatingSession()
        }
    }

    fun setBurstSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        captureRequest?.let {
            for (item in settingsMap) {
                it.set(item.key, item.value)
            }
            setBurstSession(captureCallback)
        }
    }

    companion object {
        private const val TAG = "CameraController"
    }
}
