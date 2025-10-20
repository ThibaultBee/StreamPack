package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.CameraCaptureSessionCompatBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestBuilderWithTargets
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Encapsulates device controller and session controller.
 */
internal class CameraController(
    private val manager: CameraManager,
    private val dispatcherProvider: CameraDispatcherProvider,
    val cameraId: String,
    val dynamicRangeBuilder: DynamicRangeConfig.() -> Unit = {},
    val captureRequestBuilder: CaptureRequestBuilderWithTargets.() -> Unit = {}
) {
    private val sessionCompat = CameraCaptureSessionCompatBuilder.build(dispatcherProvider)

    private val coroutineScope = CoroutineScope(dispatcherProvider.default)
    private var isActiveJob: Job? = null

    private var deviceController: CameraDeviceController? = null
    private var sessionController: CameraSessionController? = null

    private val controllerMutex = Mutex()

    private val outputs = mutableMapOf<String, CameraSurface>()
    private val outputsMutex = Mutex()

    private val _isActiveFlow = MutableStateFlow(false)
    val isActiveFlow = _isActiveFlow.asStateFlow()

    /**
     * Whether the current capture session has the given output.
     */
    suspend fun hasOutput(output: CameraSurface): Boolean {
        return outputsMutex.withLock { outputs.values.contains(output) }
    }

    /**
     * Whether the current capture session has the given output.
     *
     * @param name The name of the output to check
     */
    suspend fun hasOutput(name: String): Boolean {
        return outputsMutex.withLock { outputs.keys.contains(name) }
    }

    /**
     * Gets an output from the current capture session.
     *
     * @param name The name of the output to get
     */
    suspend fun getOutput(name: String): CameraSurface? {
        return outputsMutex.withLock { outputs[name] }
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
        outputsMutex.withLock {
            if (outputs.values.contains(output)) {
                Logger.e(TAG, "Output is already in the current session: $output")
                return
            }
            val needRestart = isActiveFlow.value
            outputs[output.name] = output
            if (needRestart) {
                restartSession()
            }
        }
    }

    /**
     * Removes an output from the current capture session.
     *
     * If the output is in the current capture session, the capture session is recreated without.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun removeOutput(name: String): Boolean {
        return outputsMutex.withLock {
            val needsRestart = outputs.containsKey(name) && isActiveFlow.value
            outputs.remove(name) != null
            if (needsRestart) {
                restartSession()
            }
            needsRestart
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
        Logger.e(TAG, "get session controllers with output $outputs")
        return if (sessionController == null) {
            val deviceController = getDeviceController()
            CameraSessionController.create(
                sessionCompat,
                deviceController,
                outputs.values.toList(),
                dynamicRange = DynamicRangeConfig().apply(dynamicRangeBuilder).dynamicRange,
                captureRequestBuilder
            )
                .apply {
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
                        dynamicRange = DynamicRangeConfig().apply(dynamicRangeBuilder).dynamicRange
                    )
                        .apply {
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
    suspend fun restartSession() {
        controllerMutex.withLock {
            Logger.e(TAG, "restart session: outputs=$outputs")
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
                dynamicRange = DynamicRangeConfig().apply(dynamicRangeBuilder).dynamicRange,
            ).apply {
                applySessionController(this)
                Logger.d(TAG, "Session controller restarted")
            }
        }
    }

    /**
     * Adds a target to the current capture session.
     *
     * @param name The name of target to add
     * @return true if the target has been added, false otherwise
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun addTarget(name: String): Boolean {
        return controllerMutex.withLock {
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
    suspend fun addTarget(target: CameraSurface): Boolean {
        return controllerMutex.withLock {
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
    suspend fun addTargets(targets: List<CameraSurface>): Boolean {
        return controllerMutex.withLock {
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
        controllerMutex.withLock {
            val sessionController = getSessionController()
            sessionController.removeTarget(target)
            if (sessionController.isEmpty) {
                closeControllers()
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
        controllerMutex.withLock {
            val sessionController = getSessionController()
            sessionController.removeTarget(name)
            if (sessionController.isEmpty) {
                closeControllers()
            }
        }
    }

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
     * Sets a repeating session with the current capture request.
     */
    suspend fun setRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val sessionController = requireNotNull(sessionController) { "SessionController is null" }
        sessionController.setRepeatingSession(cameraCaptureCallback)
    }

    suspend fun setBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val sessionController =
            requireNotNull(sessionController) { "SessionController is null" }
        sessionController.setBurstSession(cameraCaptureCallback)
    }

    private fun closeControllers() {
        sessionController?.close()
        deviceController?.close()
        deviceController = null
    }

    fun release() {
        runBlocking {
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

    internal class DynamicRangeConfig(
        var dynamicRange: Long = DynamicRangeProfile.sdr.dynamicRange
    )
}