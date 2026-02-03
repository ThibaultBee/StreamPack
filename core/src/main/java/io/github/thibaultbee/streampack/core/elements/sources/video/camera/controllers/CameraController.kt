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

import android.Manifest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.CameraCaptureSessionCompatBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSessionCallback
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestWithTargetsBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureResultListener
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Encapsulates device controller and session controller.
 */
internal class CameraController(
    private val manager: CameraManager,
    private val characteristics: CameraCharacteristics,
    dispatcherProvider: CameraDispatcherProvider,
    val cameraId: String,
    val captureRequestBuilder: CaptureRequestWithTargetsBuilder.() -> Unit = {}
) {
    private val sessionCompat = CameraCaptureSessionCompatBuilder.build(dispatcherProvider)

    private val defaultDispatcher = dispatcherProvider.default
    private val coroutineScope = CoroutineScope(defaultDispatcher)
    private var isActiveJob: Job? = null

    private var deviceController: CameraDeviceController? = null
    private var sessionController: CameraSessionController? = null

    private val sessionCallback = CameraSessionCallback(coroutineScope)

    private val controllerMutex = Mutex()

    private val outputs = mutableMapOf<String, CameraSurface>()

    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow = _isActiveFlow.asStateFlow()

    private val fpsRange: Range<Int>
        get() = CameraUtils.getClosestFpsRange(characteristics, fps)
    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr

    /**
     * Whether the current capture session has the given output.
     */
    suspend fun hasOutput(output: CameraSurface) = withContext(defaultDispatcher) {
        controllerMutex.withLock { outputs.values.contains(output) }
    }

    /**
     * Whether the current capture session has the given output.
     *
     * @param name The name of the output to check
     */
    suspend fun hasOutput(name: String) = withContext(defaultDispatcher) {
        controllerMutex.withLock { outputs.keys.contains(name) }
    }

    /**
     * Gets an output from the current capture session.
     *
     * @param name The name of the output to get
     */
    suspend fun getOutput(name: String) = withContext(defaultDispatcher) {
        controllerMutex.withLock { outputs[name] }
    }

    /**
     * Adds an output to the current capture session.
     *
     * If the output is not in the current capture session, the capture session is recreated with
     * the new output.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun addOutput(output: CameraSurface) {
        require(output.surface.isValid) { "Output is invalid: $output" }
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                if (outputs.values.contains(output)) {
                    Logger.w(TAG, "Output is already added: $output")
                    return@withContext
                }
                outputs[output.name] = output
                if (isActiveFlow.value) {
                    restartSessionUnsafe()
                }
            }
        }
    }

    /**
     * Removes an output from the current capture session.
     *
     * If the output is in the current capture session, the capture session is recreated without.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun removeOutput(name: String) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                val needRestart = outputs.remove(name) != null && isActiveFlow.value
                if (outputs.isEmpty()) {
                    sessionController?.close()
                } else if (needRestart) {
                    restartSessionUnsafe()
                }
            }
        }
    }

    /**
     * To be used under [controllerMutex]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun getDeviceController(): CameraDeviceController {
        return if (deviceController != null && !deviceController!!.isClosed) {
            deviceController!!
        } else {
            CameraDeviceController.create(manager, sessionCompat, cameraId).apply {
                deviceController = this
                Logger.d(TAG, "Device controller created")
            }
        }
    }

    /**
     * To be used under [controllerMutex]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun getSessionController(): CameraSessionController {
        return if (sessionController == null) {
            val deviceController = getDeviceController()
            CameraSessionController.create(
                coroutineScope,
                deviceController,
                sessionCallback,
                sessionCompat,
                outputs.values.toList(),
                dynamicRange = dynamicRangeProfile.dynamicRange,
                fpsRange = fpsRange,
                captureRequestBuilder
            ).apply {
                applySessionController(this)
                Logger.d(TAG, "Session controller created")
            }
        } else {
            if (!sessionController!!.isClosed) {
                sessionController!!
            } else {
                try {
                    val deviceController = getDeviceController()
                    sessionController!!.recreate(
                        deviceController,
                        outputs.values.toList(),
                        dynamicRange = dynamicRangeProfile.dynamicRange,
                        fpsRange = fpsRange
                    ).apply {
                        applySessionController(this)
                        Logger.d(TAG, "Session controller recreated")
                    }
                } catch (t: Throwable) {
                    _isActiveFlow.tryEmit(false)
                    throw t
                }
            }
        }
    }

    private fun applySessionController(sessionController: CameraSessionController) {
        this.sessionController = sessionController

        isActiveJob = coroutineScope.launch {
            sessionController.isClosedFlow.collect {
                _isActiveFlow.emit(!it)
            }
        }
    }

    /**
     * Restarts the current capture session.
     *
     * The current capture session is closed and a new one is created with the same outputs.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun restartSessionUnsafe() {
        val sessionController = sessionController
        if (sessionController == null) {
            Logger.i(TAG, "SessionController is null, nothing to restart")
            return
        }

        isActiveJob?.cancel()
        isActiveJob = null

        sessionController.recreate(
            getDeviceController(),
            outputs.values.toList(),
            dynamicRange = dynamicRangeProfile.dynamicRange,
            fpsRange = fpsRange
        ).apply {
            applySessionController(this)
            Logger.d(TAG, "Session controller restarted")
        }
    }

    /**
     * Adds a target to the current capture session.
     *
     * @param name The name of target to add
     * @return true if the target has been added, false otherwise
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun addTarget(name: String) = withContext(defaultDispatcher) {
        controllerMutex.withLock {
            val sessionController = getSessionController()
            sessionController.addTarget(name)
        }
    }

    /**
     * Adds a target to the current capture session.
     *
     * @param target The target to add
     * @return true if the target has been added, false otherwise
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun addTarget(target: CameraSurface) = withContext(defaultDispatcher) {
        controllerMutex.withLock {
            val sessionController = getSessionController()
            sessionController.addTarget(target)
        }
    }

    /**
     * Adds targets to the current capture session.
     *
     * @param targets The targets to add
     * @return true if the targets have been added, false otherwise
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun addTargets(targets: List<CameraSurface>) = withContext(defaultDispatcher) {
        controllerMutex.withLock {
            val sessionController = getSessionController()
            sessionController.addTargets(targets)
        }
    }

    /**
     * Removes a target from the current capture session.
     *
     * @param target The target to remove
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun removeTarget(target: CameraSurface) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                val sessionController = getSessionController()
                sessionController.removeTarget(target)
                if (sessionController.isEmpty()) {
                    closeControllers()
                }
            }
        }
    }

    /**
     * Removes a target from the current capture session.
     *
     * @param name The name of target to remove
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun removeTarget(name: String) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                val sessionController = getSessionController()
                sessionController.removeTarget(name)
                if (sessionController.isEmpty()) {
                    closeControllers()
                }
            }
        }
    }

    /**
     * Gets a setting from the current capture request.
     */
    fun <T> getSetting(key: CaptureRequest.Key<T?>): T? {
        val sessionController = requireNotNull(sessionController) { "SessionController is null" }
        return sessionController.getSetting(key)
    }

    /**
     * Sets a setting to the current capture request.
     *
     * Don't forget to call [setRepeatingSession] to apply the setting.
     *
     * @param key The setting key
     * @param value The setting value
     */
    fun <T> setSetting(key: CaptureRequest.Key<T>, value: T) {
        val sessionController = requireNotNull(sessionController) { "SessionController is null" }
        sessionController.setSetting(key, value)
    }

    /**
     * Sets the fps to the current capture request.
     *
     * @param fpsRange The fps range
     */
    suspend fun setFps(fps: Int) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                if (this@CameraController.fps == fps) {
                    return@withContext
                }

                this@CameraController.fps = fps

                if (isActiveFlow.value) {
                    val range = fpsRange
                    val minFrameDuration = 1_000_000_000 / range.upper.toLong()
                    setSetting(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
                    setSetting(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDuration)
                    setRepeatingSession()
                }
            }
        }
    }

    /**
     * Sets the dynamic range profile to the current capture request.
     *
     * @param dynamicRangeProfile The dynamic range profile
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun setDynamicRangeProfile(dynamicRangeProfile: DynamicRangeProfile) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                if (this@CameraController.dynamicRangeProfile == dynamicRangeProfile) {
                    return@withContext
                }
                Logger.d(TAG, "Setting dynamic range profile to $dynamicRangeProfile")

                this@CameraController.dynamicRangeProfile = dynamicRangeProfile

                if (isActiveFlow.value) {
                    restartSessionUnsafe()
                }
            }
        }
    }

    /**
     * Adds a capture callback listener to the current capture session.
     *
     * The listener is removed when it returns true or [removeCaptureCallbackListener] is called.
     */
    suspend fun addCaptureCallbackListener(listener: CaptureResultListener) {
        sessionCallback.addListener(listener)
    }

    /**
     * Removes a capture callback listener from the current capture session.
     *
     * @param listener The listener to remove
     */
    suspend fun removeCaptureCallbackListener(listener: CaptureResultListener) {
        sessionCallback.removeListener(listener)
    }

    /**
     * Sets a repeating session with the current capture request.
     *
     * @param tag A tag to associate with the session.
     */
    suspend fun setRepeatingSession(tag: Any? = null) {
        val sessionController = requireNotNull(sessionController) { "SessionController is null" }
        sessionController.setRepeatingSession(tag)
    }

    private suspend fun closeControllers() {
        sessionController?.close()
        deviceController?.close()
        deviceController = null
    }

    suspend fun release() {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                closeControllers()
                sessionController = null
            }
        }
        outputs.clear()
        sessionCompat.release()
        coroutineScope.cancel()
    }

    fun muteVibrationAndSound() {
        deviceController?.muteVibrationAndSound()
    }

    fun unmuteVibrationAndSound() {
        deviceController?.unmuteVibrationAndSound()
    }

    companion object {
        private const val TAG = "CameraController"
    }
}