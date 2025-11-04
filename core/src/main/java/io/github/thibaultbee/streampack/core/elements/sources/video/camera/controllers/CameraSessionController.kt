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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestWithTargetsBuilder
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CameraSessionController private constructor(
    private val captureRequestBuilder: CaptureRequestWithTargetsBuilder,
    private val sessionCompat: ICameraCaptureSessionCompat,
    private val captureSession: CameraCaptureSession,
    private val outputs: List<CameraSurface>,
    val dynamicRange: Long,
    val isClosedFlow: StateFlow<Boolean>
) {
    private val captureSessionMutex = Mutex()

    val isClosed: Boolean
        get() = isClosedFlow.value

    private val requestTargetMutex = Mutex()

    /**
     * A default capture callback that logs the failure reason.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Logger.e(TAG, "Capture failed with code ${failure.reason}")
        }
    }

    val isEmpty: Boolean
        get() = runBlocking { requestTargetMutex.withLock { captureRequestBuilder.isEmpty } }

    /**
     * Whether the current capture request has a target
     *
     * The target must be in the current capture session, see [hasOutput].
     *
     * @param surface The target to check
     * @return true if the target is in the current capture request, false otherwise
     */
    suspend fun hasTarget(surface: Surface) = requestTargetMutex.withLock {
        captureRequestBuilder.hasTarget(surface)
    }

    /**
     * Whether the current capture request has a target
     *
     * The target must be in the current capture session, see [hasOutput].
     *
     * @param cameraSurface The target to check
     * @return true if the target is in the current capture request, false otherwise
     */
    suspend fun hasTarget(cameraSurface: CameraSurface) = requestTargetMutex.withLock {
        captureRequestBuilder.hasTarget(cameraSurface)
    }

    /**
     * Adds targets to the current capture session
     *
     * @param targets The targets to add
     */
    suspend fun addTargets(targets: List<CameraSurface>): Boolean {
        require(targets.isNotEmpty()) { "At least one target is required" }
        require(targets.all { it.surface.isValid }) { "All targets must be valid" }
        require(targets.all { outputs.contains(it) }) { "Targets must be in the current capture session: $targets ($outputs)" }

        val res = requestTargetMutex.withLock {
            val res = targets.map {
                captureRequestBuilder.addTarget(it)
            }.all { it }
            setRepeatingSession()
            res
        }

        return res
    }

    /**
     * Adds a target to the current capture session
     *
     * @param name The name of target to add
     */
    suspend fun addTarget(name: String): Boolean {
        require(outputs.any { it.name == name }) { "Target type must be in the current capture session: $name ($outputs)" }

        val res = requestTargetMutex.withLock {
            val target = outputs.first { it.name == name }
            val res = captureRequestBuilder.addTarget(target)
            setRepeatingSession()
            res
        }
        return res
    }

    /**
     * Adds a target to the current capture session
     *
     * @param target The target to add
     */
    suspend fun addTarget(target: CameraSurface): Boolean {
        require(target.surface.isValid) { "Target must be valid: $target" }
        require(outputs.contains(target)) { "Target must be in the current capture session: $target ($outputs)" }

        val res = requestTargetMutex.withLock {
            val res = captureRequestBuilder.addTarget(target)
            setRepeatingSession()
            res
        }
        return res
    }

    /**
     * Removes targets from the current capture session
     *
     * @param targets The targets to remove
     */
    suspend fun removeTargets(targets: List<CameraSurface>) {
        requestTargetMutex.withLock {
            targets.forEach {
                captureRequestBuilder.removeTarget(it)
            }
            if (captureRequestBuilder.isEmpty) {
                stopRepeatingSession()
            } else {
                setRepeatingSession()
            }
        }
    }

    /**
     * Removes a target from the current capture session
     *
     * @param name The name of target to remove
     */
    suspend fun removeTarget(name: String) {
        requestTargetMutex.withLock {
            val target = outputs.firstOrNull { it.name == name }
            target?.let {
                captureRequestBuilder.removeTarget(it)
            } ?: Logger.w(TAG, "Target type $name not found in current outputs $outputs")

            if (captureRequestBuilder.isEmpty) {
                stopRepeatingSession()
            } else {
                setRepeatingSession()
            }
        }
    }

    /**
     * Removes a target from the current capture session
     *
     * @param target The target to remove
     */
    suspend fun removeTarget(target: CameraSurface) {
        requestTargetMutex.withLock {
            captureRequestBuilder.removeTarget(target)

            if (captureRequestBuilder.isEmpty) {
                stopRepeatingSession()
            } else {
                setRepeatingSession()
            }
        }
    }

    fun close() = runBlocking {
        captureSessionMutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Session already closed")
                return@runBlocking
            }
            try {
                captureSession.close()

                if (!isClosedFlow.value) {
                    isClosedFlow.first { it }
                }
            } catch (t: Throwable) {
                Logger.w(TAG, "Error closing camera session: $t")
            }
        }
    }

    /**
     * Sets a repeating session with the current capture request.
     */
    suspend fun setRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        if (captureRequestBuilder.isEmpty) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        captureSessionMutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Camera session controller is released")
                return
            }

            sessionCompat.setRepeatingSingleRequest(
                captureSession, captureRequestBuilder.build(), cameraCaptureCallback
            )
        }
    }

    suspend fun setBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        if (captureRequestBuilder.isEmpty) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        captureSessionMutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Camera session controller is released")
                return
            }

            sessionCompat.captureBurstRequests(
                captureSession, listOf(captureRequestBuilder.build()), cameraCaptureCallback
            )
        }
    }

    private suspend fun stopRepeatingSession() {
        captureSessionMutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Camera session controller is released")
                return
            }

            captureSession.stopRepeating()
        }
    }

    /**
     * Gets a setting from the current capture request.
     */
    fun <T> getSetting(key: CaptureRequest.Key<T?>) = captureRequestBuilder.get(key)

    /**
     * Sets a setting to the current capture request.
     *
     * Don't forget to call [setRepeatingSession] to apply the setting.
     *
     * @param key The setting key
     * @param value The setting value
     */
    fun <T> setSetting(key: CaptureRequest.Key<T>, value: T) = captureRequestBuilder.set(key, value)

    /**
     * Sets a setting to the current capture request and apply it.
     *
     * @param key The setting key
     * @param value The setting value
     */
    suspend fun <T> setRepeatingSetting(key: CaptureRequest.Key<T>, value: T) {
        captureRequestBuilder.set(key, value)
        setRepeatingSession()
    }

    /**
     * Creates a new capture session with the given outputs.
     *
     * The capture session of the current [CameraSessionController] will be closed.
     *
     * @param cameraDeviceController The [CameraDeviceController] to use.
     * @param outputs The outputs to use. By default it uses the current outputs.
     * @param dynamicRange The dynamic range to use
     * @return A new [CameraSessionController]
     */
    suspend fun recreate(
        cameraDeviceController: CameraDeviceController,
        outputs: List<CameraSurface>,
        dynamicRange: Long,
        fpsRange: Range<Int>
    ): CameraSessionController = requestTargetMutex.withLock {
        require(outputs.isNotEmpty()) { "At least one output is required" }
        require(outputs.all { it.surface.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.surface.isValid }} is invalid" }

        if ((dynamicRange == this.dynamicRange) && (outputs == this.outputs) && !isClosed) {
            Logger.w(TAG, "Same dynamic range and outputs, returning the same controller")
            return this
        }

        // Re-add targets that are in the new outputs (identified by their name)
        val targets = outputs.filter {
            captureRequestBuilder.hasTarget(it.name)
        }
        captureRequestBuilder.clearTargets()
        captureRequestBuilder.addTargets(targets)
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)

        // Close current session
        close()

        val isClosedFlow = MutableStateFlow(false)
        val newCaptureSession =
            CameraUtils.createCaptureSession(
                cameraDeviceController,
                outputs.map { it.surface },
                dynamicRange,
                isClosedFlow
            )

        val controller = CameraSessionController(
            captureRequestBuilder,
            sessionCompat,
            newCaptureSession,
            outputs,
            dynamicRange,
            isClosedFlow.asStateFlow()
        )

        if (!captureRequestBuilder.isEmpty) {
            controller.setRepeatingSession()
        }

        return controller
    }

    companion object {
        private const val TAG = "CameraSessionController"

        internal suspend fun create(
            sessionCompat: ICameraCaptureSessionCompat,
            cameraDeviceController: CameraDeviceController,
            outputs: List<CameraSurface>,
            dynamicRange: Long,
            fpsRange: Range<Int>,
            defaultRequestBuilder: CaptureRequestWithTargetsBuilder.() -> Unit = {}
        ): CameraSessionController {
            require(outputs.isNotEmpty()) { "At least one output is required" }
            require(outputs.all { it.surface.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.surface.isValid }} is invalid" }

            val isClosedFlow = MutableStateFlow(false)
            val captureSession =
                CameraUtils.createCaptureSession(
                    cameraDeviceController,
                    outputs.map { it.surface },
                    dynamicRange,
                    isClosedFlow
                )

            val captureRequestBuilder =
                CaptureRequestWithTargetsBuilder.create(
                    cameraDeviceController
                ).apply {
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                    defaultRequestBuilder()
                }
            return CameraSessionController(
                captureRequestBuilder,
                sessionCompat,
                captureSession,
                outputs,
                dynamicRange,
                isClosedFlow.asStateFlow()
            )
        }
    }
}
