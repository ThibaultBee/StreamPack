package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.CameraCaptureSessionCompatBuilder
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.utils.mapState
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Encapsulates device controller and session controller.
 */
class CameraController private constructor(
    private val manager: CameraManager,
    private val camera: CameraDeviceController,
    private val sessionCompat: ICameraCaptureSessionCompat,
    private val isClosedFlow: StateFlow<Boolean>
) {

    val cameraId: String = camera.id

    private val sessionController = MutableStateFlow<CameraSessionController?>(null)

    val isAvailableFlow: StateFlow<Boolean> = sessionController.mapState { it != null }

    private val mutex = Mutex()

    /**
     * Creates a new capture session with the given outputs.
     */
    suspend fun createSessionController(outputs: List<Surface>, dynamicRange: Long) {
        require(outputs.isNotEmpty()) { "Outputs is empty" }
        require(outputs.all { it.isValid }) { "At least one output is invalid: ${outputs.filter { !it.isValid }}" }
        mutex.withLock {
            sessionController.emit(
                CameraSessionController.create(
                    sessionCompat,
                    manager,
                    camera,
                    outputs,
                    dynamicRange
                )
            )
        }
    }

    /**
     * Sets the dynamic range of the camera.
     *
     * Internally, it creates a new capture session with the given dynamic range.
     */
    suspend fun setDynamicRange(dynamicRange: Long) = mutex.withLock {
        sessionController.value?.createCameraControllerForOutputs(dynamicRange = dynamicRange)
    }

    private fun hasOutput(output: Surface): Boolean {
        return sessionController.value?.hasOutput(output) ?: false
    }

    /**
     * Adds an output to the current capture session.
     *
     * If the output is not in the current capture session, the capture session is recreated with
     * the new output.
     */
    suspend fun addOutput(output: Surface) {
        require(output.isValid) { "Output is invalid: $output" }
        mutex.withLock {
            val sessionController = sessionController.value ?: return
            if (sessionController.hasOutput(output)) {
                return
            }

            this.sessionController.emit(
                sessionController.createCameraControllerForOutputs(
                    outputs = sessionController.outputs + output
                )
            )
        }
    }

    /**
     * Removes an output from the current capture session.
     *
     * If the output is in the current capture session, the capture session is recreated without.
     */
    suspend fun removeOutput(output: Surface) {
        mutex.withLock {
            val sessionController = sessionController.value ?: return
            if (!sessionController.hasOutput(output)) {
                return
            }

            try {
                val newOutputs = sessionController.outputs - output
                if (newOutputs.isEmpty()) {
                    sessionController.release()
                    this.sessionController.emit(null)
                } else {
                    this.sessionController.emit(
                        sessionController.createCameraControllerForOutputs(
                            outputs = sessionController.outputs - output
                        )
                    )
                }
            } catch (t: Throwable) {
                Logger.e(
                    TAG,
                    "Failed to remove output $output from ${sessionController.outputs}: $t"
                )
                throw t
            }
        }
    }

    /**
     * Replaces an output in the current capture session.
     *
     * If the output is not in the current capture session, the capture session is recreated with
     * the new output.
     */
    suspend fun replaceOutput(previousOutput: Surface, newOutput: Surface) {
        require(newOutput.isValid) { "Output is invalid: $newOutput" }
        mutex.withLock {
            val sessionController = sessionController.value ?: return
            if (sessionController.hasOutput(newOutput) && !sessionController.hasOutput(
                    previousOutput
                )
            ) {
                return
            }

            this.sessionController.emit(
                sessionController.createCameraControllerForOutputs(
                    outputs = sessionController.outputs - previousOutput + newOutput
                )
            )
        }
    }

    /**
     * Adds a target to the current capture session.
     *
     * @param output The target to add
     * @return true if the target has been added, false otherwise
     */
    suspend fun addTarget(output: Surface): Boolean = mutex.withLock {
        return sessionController.value?.addTarget(output) ?: false
    }

    /**
     * Adds targets to the current capture session.
     *
     * @param targets The targets to add
     * @return true if the targets have been added, false otherwise
     */
    suspend fun addTargets(targets: List<Surface>): Boolean = mutex.withLock {
        return sessionController.value?.addTargets(targets) ?: false
    }

    /**
     * Removes a target from the current capture session.
     */
    suspend fun removeTarget(output: Surface) = mutex.withLock {
        sessionController.value?.removeTarget(output)
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
        val sessionController =
            requireNotNull(sessionController.value) { "SessionController is null" }
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
        val sessionController =
            requireNotNull(sessionController.value) { "SessionController is null" }
        sessionController.setSetting(key, value)
    }

    /**
     * Sets a repeating session with the current capture request.
     */
    fun setRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val sessionController =
            requireNotNull(sessionController.value) { "SessionController is null" }
        sessionController.setRepeatingSession(cameraCaptureCallback)
    }

    fun setBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        val sessionController =
            requireNotNull(sessionController.value) { "SessionController is null" }
        sessionController.setBurstSession(cameraCaptureCallback)
    }

    fun release() {
        runBlocking {
            mutex.withLock {
                sessionController.value?.release()
                sessionController.emit(null)
            }
        }
        camera.close()
        runBlocking {
            isClosedFlow.first()
        }
        sessionCompat.release()
    }

    fun muteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                camera.cameraAudioRestriction = CameraDevice.AUDIO_RESTRICTION_VIBRATION_SOUND
            } catch (t: Throwable) {
                Logger.w(TAG, "Failed to mute vibration and sound $t")
            }
        }
    }

    fun unmuteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                camera.cameraAudioRestriction = CameraDevice.AUDIO_RESTRICTION_NONE
            } catch (t: Throwable) {
                Logger.w(TAG, "Failed to unmute vibration and sound: $t")
            }
        }
    }

    companion object {
        private const val TAG = "CameraController"

        internal suspend fun create(
            manager: CameraManager,
            cameraId: String
        ): CameraController {
            val isClosedFlow = MutableStateFlow(false)
            val sessionCompat = CameraCaptureSessionCompatBuilder.build()
            val cameraDevice = CameraUtils.openCamera(
                CameraCaptureSessionCompatBuilder.build(),
                manager,
                cameraId,
                isClosedFlow
            )
            return CameraController(
                manager,
                CameraDeviceController(cameraDevice),
                sessionCompat,
                isClosedFlow.asStateFlow()
            )
        }
    }
}