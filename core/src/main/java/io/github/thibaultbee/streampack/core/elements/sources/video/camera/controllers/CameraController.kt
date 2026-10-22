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
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraException
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.dynamicRangeProfiles
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isCapabilitiesSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFpsSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.CameraCaptureSessionCompatBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSessionCallback
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestWithTargetsBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureResultListener
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
    val onCaptureRequestBuilder: CaptureRequestWithTargetsBuilder.() -> Unit = {}
) {
    private val sessionCompat = CameraCaptureSessionCompatBuilder.build(dispatcherProvider)

    private val defaultDispatcher = dispatcherProvider.default
    private val coroutineScope = CoroutineScope(defaultDispatcher)
    private var isActiveJob: Job? = null

    private var deviceController: CameraDeviceController? = null
    private var sessionController: CameraSessionController? = null

    private var isReleased = false

    private var captureRequestBuilder: CaptureRequestWithTargetsBuilder? = null
    private val sessionCallback = CameraSessionCallback(coroutineScope)

    private val controllerMutex = Mutex()

    private val outputs = mutableMapOf<String, CameraSurface>()

    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow = _isActiveFlow.asStateFlow()

    private val isActive: Boolean
        get() = isActiveFlow.value && sessionController?.isClosed == false && deviceController?.isClosed == false

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
                val previousOutput = outputs.put(output.name, output)
                if (isActive) {
                    try {
                        restartSessionUnsafe()
                    } catch (t: Throwable) {
                        if (previousOutput != null) {
                            outputs[output.name] = previousOutput
                        } else {
                            outputs.remove(output.name)
                        }
                        throw t
                    }
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
                if (isReleased) {
                    return@withContext
                }
                val removedOutput = outputs.remove(name) ?: return@withContext
                captureRequestBuilder?.removeTarget(removedOutput)
                if (outputs.isEmpty() || captureRequestBuilder?.isEmpty() != false) {
                    sessionController?.close()
                } else if (isActive) {
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
        if (isReleased) {
            throw IllegalStateException("CameraController is already released")
        }
        return if (deviceController != null && !deviceController!!.isClosed) {
            deviceController!!
        } else {
            CameraDeviceController.create(manager, sessionCompat, cameraId).apply {
                deviceController = this
                Logger.d(TAG, "Device controller created")
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun getCaptureRequestBuilder(): CaptureRequestWithTargetsBuilder {
        return if (captureRequestBuilder != null) {
            captureRequestBuilder!!
        } else {
            getDeviceController().createCaptureRequestBuilder().apply {
                captureRequestBuilder = this

                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                if (characteristics.isCapabilitiesSupported(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)) {
                    val minFrameDuration = 1_000_000_000 / fpsRange.upper.toLong()
                    set(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDuration)
                }
                onCaptureRequestBuilder()
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun getSessionParameters(): CaptureRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getCaptureRequestBuilder().build()
        } else {
            null
        }
    }

    /**
     * To be used under [controllerMutex]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun getSessionController(): CameraSessionController {
        return if (sessionController == null) {
            CameraSessionController.create(
                getDeviceController(),
                sessionCallback,
                sessionCompat,
                outputs.values.toList(),
                dynamicRange = dynamicRangeProfile.dynamicRange,
                sessionParameters = getSessionParameters(),
                coroutineDispatcher = defaultDispatcher
            ).apply {
                applySessionController(this)
                Logger.d(TAG, "Session controller created")
            }
        } else {
            if (!sessionController!!.isClosed) {
                sessionController!!
            } else {
                try {
                    sessionController!!.recreate(
                        getDeviceController(),
                        outputs.values.toList(),
                        dynamicRange = dynamicRangeProfile.dynamicRange,
                        sessionParameters = getSessionParameters()
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

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun applySessionController(
        sessionController: CameraSessionController
    ) {
        this.sessionController = sessionController

        isActiveJob?.cancel()
        isActiveJob = coroutineScope.launch {
            combine(
                sessionController.isClosedFlow,
                sessionController.cameraIsClosedFlow
            ) { sessionClosed, cameraClosed ->
                !sessionClosed && !cameraClosed
            }.collect { isActive ->
                _isActiveFlow.emit(isActive)
            }
        }

        // Re-add targets that are in the new outputs (identified by their name)
        captureRequestBuilder?.let {
            val targets = outputs.values.filter { output ->
                it.hasTarget(output.name)
            }
            it.clearTargets()
            if (targets.isNotEmpty()) {
                it.addTargets(targets)
            }
        }

        applyRepeatingSessionUnsafe()
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
            sessionParameters = getSessionParameters()
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
            val target = outputs[name] ?: throw IllegalArgumentException(
                "Output $name not found in outputs ${outputs.keys.joinToString(", ")}"
            )

            val wasAdded = getCaptureRequestBuilder().addTarget(target)
            if (wasAdded) {
                try {
                    applyRepeatingSessionSyncUnsafe()
                } catch (t: Throwable) {
                    getCaptureRequestBuilder().removeTarget(target)
                    Logger.e(TAG, "Error to add target $name", t)
                    throw t
                }
                Logger.d(TAG, "Target $name added")
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
                if (isReleased) {
                    return@withContext
                }
                val target = outputs[name] ?: return@withContext

                val wasRemoved = getCaptureRequestBuilder().removeTarget(target)
                if (!wasRemoved) {
                    return@withLock
                }
                try {
                    applyRepeatingSessionUnsafe()
                } catch (t: Throwable) {
                    Logger.e(TAG, "Error to remove target $name", t)
                }
                Logger.d(TAG, "Target $name removed")
            }
        }
    }

    /**
     * Gets a setting from the current capture request.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    fun <T> getSetting(key: CaptureRequest.Key<T?>): T? {
        val captureRequestBuilder =
            requireNotNull(captureRequestBuilder) { "CaptureRequestBuilder is null" }
        return captureRequestBuilder.get(key)
    }

    /**
     * Resets the current capture request to its default settings.
     *
     * Don't forget to call [applyRepeatingSession] to apply the setting.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun resetSettings() {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                captureRequestBuilder ?: return@withLock
                val currentCaptureRequestBuilder = getCaptureRequestBuilder()
                // Get targets of previous builder
                val targets = currentCaptureRequestBuilder.getTargets()
                captureRequestBuilder = null
                getCaptureRequestBuilder().addTargets(targets)
            }
        }
    }

    /**
     * Sets a setting to the current capture request.
     *
     * Don't forget to call [applyRepeatingSession] to apply the setting.
     *
     * @param key The setting key
     * @param value The setting value
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun <T> setSetting(key: CaptureRequest.Key<T>, value: T) {
        val captureRequestBuilder = getCaptureRequestBuilder()
        captureRequestBuilder.set(key, value)
    }

    /**
     * Sets the fps to the current capture request.
     *
     * @param fps The fps
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun setFps(fps: Int) {
        require(characteristics.isFpsSupported(fps)) {
            "Camera $cameraId does not support $fps fps"
        }
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                if (this@CameraController.fps == fps) {
                    return@withContext
                }
                Logger.d(TAG, "Setting fps to $fps")

                val previousFps = this@CameraController.fps
                this@CameraController.fps = fps

                val captureRequestBuilder = getCaptureRequestBuilder()
                val previousRange =
                    captureRequestBuilder.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
                val previousMinFrameDuration =
                    captureRequestBuilder.get(CaptureRequest.SENSOR_FRAME_DURATION)

                val range = fpsRange
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
                if (characteristics.isCapabilitiesSupported(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)) {
                    val minFrameDuration = 1_000_000_000 / range.upper.toLong()
                    captureRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, minFrameDuration)
                }

                try {
                    val validOutputs = outputs.values.map { it.surface }.filter { it.isValid }
                    if (validOutputs.isNotEmpty()) {
                        val isSupported = CameraUtils.isSessionConfigurationSupported(
                            getDeviceController(),
                            validOutputs,
                            dynamicRangeProfile.dynamicRange,
                            getSessionParameters()
                        )
                        if (!isSupported) {
                            throw CameraException("Session configuration with $fps fps ($range) is not supported for outputs $validOutputs")
                        }
                    }

                    if (isActive) {
                        applyRepeatingSessionUnsafe()
                    }
                } catch (t: Throwable) {
                    this@CameraController.fps = previousFps
                    if (previousRange != null) {
                        captureRequestBuilder.set(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            previousRange
                        )
                    }
                    if (previousMinFrameDuration != null) {
                        captureRequestBuilder.set(
                            CaptureRequest.SENSOR_FRAME_DURATION,
                            previousMinFrameDuration
                        )
                    }
                    throw t
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
        require(characteristics.dynamicRangeProfiles.contains(dynamicRangeProfile.dynamicRange)) {
            "Camera $cameraId does not support dynamic range profile $dynamicRangeProfile"
        }
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                if (this@CameraController.dynamicRangeProfile == dynamicRangeProfile) {
                    return@withContext
                }
                Logger.d(TAG, "Setting dynamic range profile to $dynamicRangeProfile")

                val previousProfile = this@CameraController.dynamicRangeProfile
                this@CameraController.dynamicRangeProfile = dynamicRangeProfile

                try {
                    if (isActive) {
                        restartSessionUnsafe()
                    } else {
                        val validOutputs = outputs.values.map { it.surface }.filter { it.isValid }
                        if (validOutputs.isNotEmpty()) {
                            val isSupported = CameraUtils.isSessionConfigurationSupported(
                                getDeviceController(),
                                validOutputs,
                                dynamicRangeProfile.dynamicRange,
                                getSessionParameters()
                            )
                            if (!isSupported) {
                                throw CameraException("Session configuration with dynamic range $dynamicRangeProfile is not supported for outputs $validOutputs")
                            }
                        }
                    }
                } catch (t: Throwable) {
                    this@CameraController.dynamicRangeProfile = previousProfile
                    throw t
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
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun applyRepeatingSessionUnsafe(tag: Any? = null) {
        val sessionController = getSessionController()
        val captureRequestBuilder = getCaptureRequestBuilder()
        tag?.let {
            captureRequestBuilder.setTag(it)
        }
        sessionController.applyRepeatingSession(captureRequestBuilder)
    }


    /**
     * Sets a repeating session with the current capture request.
     *
     * @param tag A tag to associate with the session.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun applyRepeatingSession(tag: Any? = null) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                applyRepeatingSessionUnsafe(tag)
            }
        }
    }

    /**
     * Applies settings to the camera repeatedly.
     *
     * @param onCaptureResult the capture result callback. Return `true` to stop the callback.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun applyRepeatingSession(onCaptureResult: CaptureResultListener) {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                applyRepeatingSessionUnsafe(onCaptureResult)
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun applyRepeatingSessionUnsafe(onCaptureResult: CaptureResultListener): CaptureResultListener {
        val tag = TagBundle.Factory.default.create()
        val captureCallback = object : CaptureResultListener {
            override fun onCaptureResult(result: TotalCaptureResult): Boolean {
                val resultTag = result.request.tag as? TagBundle
                val keyId = resultTag?.keyId ?: return false
                return keyId >= tag.keyId && onCaptureResult.onCaptureResult(result)
            }

            override fun onCaptureFailed(failure: CaptureFailure): Boolean {
                Logger.e(TAG, "Capture request failed: reason=${failure.reason}")
                val failureTag = failure.request.tag as? TagBundle
                val keyId = failureTag?.keyId ?: return false
                return keyId >= tag.keyId && onCaptureResult.onCaptureFailed(failure)
            }
        }

        addCaptureCallbackListener(captureCallback)
        applyRepeatingSessionUnsafe(tag)
        return captureCallback
    }

    /**
     * Applies settings to the camera repeatedly in a synchronized way.
     *
     * This method returns when the capture callback is received with the passed request.
     *
     * @return the total capture result
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun applyRepeatingSessionSync(): TotalCaptureResult =
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                applyRepeatingSessionSyncUnsafe()
            }
        }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun applyRepeatingSessionSyncUnsafe(): TotalCaptureResult {
        val captureRequestBuilder = getCaptureRequestBuilder()
        if (captureRequestBuilder.isEmpty()) {
            throw IllegalStateException("Cannot synchronize repeating session without targets")
        }

        val deviceController = getDeviceController()
        if (deviceController.isClosed) {
            val error = deviceController.throwableFlow.value
                ?: CameraException("Camera device is closed")
            throw error
        }

        val sessionController = getSessionController()
        if (sessionController.isClosed) {
            val error = deviceController.throwableFlow.value
                ?: CameraException("Camera session is closed")
            throw error
        }

        val deferred = CompletableDeferred<TotalCaptureResult>()

        val captureResult = object : CaptureResultListener {
            override fun onCaptureResult(result: TotalCaptureResult): Boolean {
                deferred.complete(result)
                return true
            }

            override fun onCaptureFailed(failure: CaptureFailure): Boolean {
                deferred.completeExceptionally(
                    CameraException("Capture request failed: reason=${failure.reason}")
                )
                return true
            }
        }

        val errorJob = coroutineScope.launch {
            combine(
                deviceController.throwableFlow,
                deviceController.isClosedFlow,
                sessionController.isClosedFlow
            ) { throwable, deviceClosed, sessionClosed ->
                when {
                    throwable != null -> throwable
                    deviceClosed -> CameraException("Camera device has been closed")
                    sessionClosed -> CameraException("Camera session has been closed")
                    else -> null
                }
            }.filterNotNull().collect { error ->
                deferred.completeExceptionally(error)
            }
        }

        val registeredListener = applyRepeatingSessionUnsafe(captureResult)
        try {
            return deferred.await()
        } finally {
            errorJob.cancel()
            removeCaptureCallbackListener(registeredListener)
        }
    }

    private class TagBundle(val keyId: Long) {
        private val tagMap = mutableMapOf<String, Any?>().apply {
            put(TAG_KEY_ID, keyId)
        }

        val keys: Set<String>
            get() = tagMap.keys

        fun setTag(key: String, value: Any?) {
            tagMap[key] = value
        }

        companion object {
            private const val TAG_KEY_ID = "TAG_KEY_ID"
        }

        /**
         * Factory for [TagBundle].
         *
         * The purpose is to make sure the tag always contains an increasing id.
         */
        class Factory private constructor() {
            /**
             * Next session id.
             */
            private val nextSessionUpdateId = AtomicLong(0)

            fun create(): TagBundle {
                return TagBundle(nextSessionUpdateId.getAndIncrement())
            }

            companion object {
                val default = Factory()
            }
        }
    }

    private suspend fun closeControllers() {
        sessionController?.close()
        sessionController = null
        deviceController?.close()
        deviceController = null
        captureRequestBuilder = null
    }

    suspend fun release() {
        withContext(defaultDispatcher) {
            controllerMutex.withLock {
                isReleased = true
                closeControllers()
                outputs.clear()
            }
        }
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