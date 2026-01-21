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
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.util.Range
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestWithTargetsBuilder
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class CameraSessionController private constructor(
    private val coroutineScope: CoroutineScope,
    private val captureSession: CameraCaptureSession,
    private val captureRequestBuilder: CaptureRequestWithTargetsBuilder,
    private val sessionCallback: CameraControlSessionCallback,
    private val sessionCompat: ICameraCaptureSessionCompat,
    private val outputs: List<CameraSurface>,
    val dynamicRange: Long,
    val cameraIsClosedFlow: StateFlow<Boolean>,
    val isClosedFlow: StateFlow<Boolean>
) {
    private val captureSessionMutex = Mutex()

    val isClosed: Boolean
        get() = isClosedFlow.value || cameraIsClosedFlow.value

    private val requestTargetMutex = Mutex()

    /**
     * A default capture callback that logs the failure reason.
     */
    private val captureCallback = object : CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            Logger.e(TAG, "Capture failed with code ${failure.reason}")
        }
    }

    private val captureCallbacks =
        setOf(captureCallback, sessionCallback)

    suspend fun isEmpty() = withContext(coroutineScope.coroutineContext) {
        requestTargetMutex.withLock { captureRequestBuilder.isEmpty() }
    }

    /**
     * Whether the current capture request has a target
     *
     * @param surface The target to check
     * @return true if the target is in the current capture request, false otherwise
     */
    suspend fun hasTarget(surface: Surface) = withContext(coroutineScope.coroutineContext) {
        requestTargetMutex.withLock {
            captureRequestBuilder.hasTarget(surface)
        }
    }

    /**
     * Whether the current capture request has a target
     *
     * @param cameraSurface The target to check
     * @return true if the target is in the current capture request, false otherwise
     */
    suspend fun hasTarget(cameraSurface: CameraSurface) =
        withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                captureRequestBuilder.hasTarget(cameraSurface)
            }
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

        val res = withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                val res = targets.map {
                    captureRequestBuilder.addTarget(it)
                }.all { it }
                setRepeatingSession()
                res
            }
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

        val res = withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                val target = outputs.first { it.name == name }
                val res = captureRequestBuilder.addTarget(target)
                setRepeatingSession()
                res
            }
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

        val res = withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                val res = captureRequestBuilder.addTarget(target)
                setRepeatingSession()
                res
            }
        }
        return res
    }

    /**
     * Removes targets from the current capture session
     *
     * @param targets The targets to remove
     */
    suspend fun removeTargets(targets: List<CameraSurface>) {
        withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                targets.forEach {
                    captureRequestBuilder.removeTarget(it)
                }
                if (captureRequestBuilder.isEmpty()) {
                    stopRepeatingSession()
                } else {
                    setRepeatingSession()
                }
            }
        }
    }

    /**
     * Removes a target from the current capture session
     *
     * @param name The name of target to remove
     */
    suspend fun removeTarget(name: String) {
        withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                val target = outputs.firstOrNull { it.name == name }
                target?.let {
                    captureRequestBuilder.removeTarget(it)
                } ?: Logger.w(
                    TAG,
                    "Target type $name not found in current outputs $outputs"
                )

                if (captureRequestBuilder.isEmpty()) {
                    stopRepeatingSession()
                } else {
                    setRepeatingSession()
                }
            }
        }
    }

    /**
     * Removes a target from the current capture session
     *
     * @param target The target to remove
     */
    suspend fun removeTarget(target: CameraSurface) {
        withContext(coroutineScope.coroutineContext) {
            requestTargetMutex.withLock {
                captureRequestBuilder.removeTarget(target)

                if (captureRequestBuilder.isEmpty()) {
                    stopRepeatingSession()
                } else {
                    setRepeatingSession()
                }
            }
        }
    }

    suspend fun close() {
        withContext(coroutineScope.coroutineContext) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Session already closed")
                    return@withContext
                }
                try {
                    captureSession.close()
                    combine(isClosedFlow, cameraIsClosedFlow) { isClosed, isClosedCamera ->
                        isClosed || isClosedCamera
                    }.first { it }
                } catch (t: Throwable) {
                    Logger.w(TAG, "Error closing camera session: $t")
                }
            }
        }
    }

    /**
     * Adds a capture callback listener to the current capture session.
     *
     * The listener is removed when it returns true or [removeCaptureCallbackListener] is called.
     */
    fun addCaptureCallbackListener(listener: CaptureResultListener) {
        sessionCallback.addListener(listener)
    }

    /**
     * Removes a capture callback listener from the current capture session.
     *
     * @param listener The listener to remove
     */
    fun removeCaptureCallbackListener(listener: CaptureResultListener) {
        sessionCallback.removeListener(listener)
    }

    /**
     * Sets a repeating session with the current capture request.
     *
     * @param tag A tag to associate with the session.
     */
    suspend fun setRepeatingSession(tag: Any? = null) {
        if (captureRequestBuilder.isEmpty()) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        withContext(coroutineScope.coroutineContext) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Camera session controller is released")
                    return@withContext
                }

                tag?.let { captureRequestBuilder.setTag(it) }

                sessionCompat.setRepeatingSingleRequest(
                    captureSession,
                    captureRequestBuilder.build(),
                    MultiCaptureCallback(captureCallbacks)
                )
            }
        }
    }

    private suspend fun stopRepeatingSession() {
        withContext(coroutineScope.coroutineContext) {
            captureSessionMutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Camera session controller is released")
                    return@withContext
                }

                captureSession.stopRepeating()
            }
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
    fun <T> setSetting(key: CaptureRequest.Key<T>, value: T) =
        captureRequestBuilder.set(key, value)

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
    ): CameraSessionController = withContext(coroutineScope.coroutineContext) {
        requestTargetMutex.withLock {
            require(outputs.isNotEmpty()) { "At least one output is required" }
            require(outputs.all { it.surface.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.surface.isValid }} is invalid" }

            if ((dynamicRange == this@CameraSessionController.dynamicRange) && (outputs == this@CameraSessionController.outputs) && !isClosed) {
                Logger.w(TAG, "Same dynamic range and outputs, returning the same controller")
                return@withContext this@CameraSessionController
            }

            // Re-add targets that are in the new outputs (identified by their name)
            val targets = outputs.filter {
                captureRequestBuilder.hasTarget(it.name)
            }
            captureRequestBuilder.clearTargets()
            captureRequestBuilder.addTargets(targets)
            val minFrameDuration = 1_000_000_000 / fpsRange.upper.toLong()
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            captureRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDuration)

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
                coroutineScope,
                newCaptureSession,
                captureRequestBuilder,
                sessionCallback,
                sessionCompat,
                outputs,
                dynamicRange,
                cameraDeviceController.isClosedFlow,
                isClosedFlow.asStateFlow()
            )

            if (!captureRequestBuilder.isEmpty()) {
                controller.setRepeatingSession()
            }

            controller
        }
    }

    companion object {
        private const val TAG = "CameraSessionController"

        suspend fun create(
            coroutineScope: CoroutineScope,
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

            val captureRequestBuilder = CaptureRequestWithTargetsBuilder.create(
                cameraDeviceController
            ).apply {
                val minFrameDuration = 1_000_000_000 / fpsRange.upper.toLong()
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                set(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDuration)
                defaultRequestBuilder()
            }
            return CameraSessionController(
                coroutineScope,
                captureSession,
                captureRequestBuilder,
                CameraControlSessionCallback(coroutineScope),
                sessionCompat,
                outputs,
                dynamicRange,
                cameraDeviceController.isClosedFlow,
                isClosedFlow.asStateFlow()
            )
        }
    }

    private class MultiCaptureCallback(
        private val callbacks: Set<CaptureCallback>
    ) : CaptureCallback() {
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
            callbacks.forEach {
                it.onCaptureStarted(session, request, timestamp, frameNumber)
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            callbacks.forEach {
                it.onCaptureCompleted(session, request, result)
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            callbacks.forEach {
                it.onCaptureFailed(session, request, failure)
            }
        }
    }

    interface CaptureResultListener {
        /**
         * Called when a capture result is received.
         *
         * @param result The capture result.
         * @return true if the listener is finished and should be removed, false otherwise.
         */
        fun onCaptureResult(result: TotalCaptureResult): Boolean
    }

    /**
     * A capture callback that wraps multiple [CaptureResultListener].
     *
     * @param coroutineScope The coroutine scope to use.
     */
    private class CameraControlSessionCallback(private val coroutineScope: CoroutineScope) :
        CaptureCallback() {
        /* synthetic accessor */
        private val resultListeners = mutableSetOf<CaptureResultListener>()

        /**
         * Adds a capture result listener.
         *
         * The listener is removed when [removeListener] is explicitly called or when [CaptureResultListener] returns true.
         */
        fun addListener(listener: CaptureResultListener) {
            resultListeners.add(listener)
        }

        fun removeListener(listener: CaptureResultListener) {
            resultListeners.remove(listener)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            coroutineScope.launch {
                val removeSet = mutableSetOf<CaptureResultListener>()
                for (listener in resultListeners) {
                    val isFinished: Boolean = listener.onCaptureResult(result)
                    if (isFinished) {
                        removeSet.add(listener)
                    }
                }
                if (!removeSet.isEmpty()) {
                    resultListeners.removeAll(removeSet)
                }
            }
        }
    }
}
