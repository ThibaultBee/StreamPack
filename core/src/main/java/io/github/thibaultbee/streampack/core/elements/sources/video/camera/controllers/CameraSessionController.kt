package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CameraSessionController private constructor(
    private val manager: CameraManager,
    val cameraId: String,
    private val captureSessionCompat: ICameraCaptureSessionCompat,
    captureSessionWithOutputs: CameraCaptureSessionWithOutputs,
    val dynamicRange: Long,
    private val isClosedFlow: StateFlow<Boolean>,
    private val captureRequestBuilder: CaptureRequestBuilderWithTargets = CaptureRequestBuilderWithTargets.create(
        captureSessionWithOutputs.session.device
    )
) {
    private val captureSession = captureSessionWithOutputs.session

    /**
     * The outputs of the current capture session
     */
    val outputs = captureSessionWithOutputs.outputs

    var isReleased = false
        private set

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

    /**
     * Whether the current capture request has an output.
     *
     * If the output is not in the current capture session, you will have to create a new capture
     * session. See [createCameraControllerForOutputs].
     *
     * @param surface The output to check
     * @return true if the output is in the current capture session, false otherwise
     */
    fun hasOutput(surface: Surface) = outputs.contains(surface)

    /**
     * Creates a new capture session with the given outputs.
     *
     * The capture session of the current [CameraSessionController] will be closed.
     *
     * @param dynamicRange The dynamic range to use
     * @param outputs The outputs to use. By default it uses the current outputs.
     * @return A new [CameraSessionController]
     */
    suspend fun createCameraControllerForOutputs(
        dynamicRange: Long = this.dynamicRange,
        outputs: List<Surface> = this.outputs
    ): CameraSessionController = requestTargetMutex.withLock {
        require(outputs.isNotEmpty()) { "At least one output is required" }
        require(outputs.all { it.isValid }) { "All outputs $outputs must be valid but ${outputs.filter { !it.isValid }} is invalid" }

        if (dynamicRange == this.dynamicRange && outputs == this.outputs) {
            return this
        }

        val isClosedFlow = MutableStateFlow(false)
        val newCaptureSession =
            CameraUtils.createCaptureSession(
                captureSessionCompat,
                captureSession.device,
                outputs,
                dynamicRange,
                isClosedFlow
            )
        captureSession.close()

        val controller = CameraSessionController(
            manager,
            cameraId,
            captureSessionCompat,
            CameraCaptureSessionWithOutputs(newCaptureSession, outputs),
            dynamicRange,
            isClosedFlow.asStateFlow(),
            captureRequestBuilder
        )

        controller.setRepeatingSession()
        return controller
    }

    /**
     * Whether the current capture request has a target
     *
     * @param surface The target to check
     * @return true if the target is in the current capture request, false otherwise
     */
    suspend fun hasTarget(surface: Surface) = requestTargetMutex.withLock {
        captureRequestBuilder.hasTarget(surface)
    }

    /**
     * Adds targets to the current capture session
     *
     * @param targets The targets to add
     */
    suspend fun addTargets(targets: List<Surface>): Boolean {
        require(targets.isNotEmpty()) { "At least one target is required" }
        require(targets.all { it.isValid }) { "All targets must be valid" }
        require(targets.all { outputs.contains(it) }) { "Targets must be in the current capture session: $targets ($outputs)" }

        val res = requestTargetMutex.withLock {
            targets.map {
                captureRequestBuilder.addTarget(it)
            }.all { it }
        }
        setRepeatingSession()
        return res
    }

    /**
     * Adds a target to the current capture session
     *
     * @param target The target to add
     */
    suspend fun addTarget(target: Surface): Boolean {
        require(target.isValid) { "Target must be valid: $target" }
        require(outputs.contains(target)) { "Target must be in the current capture session: $target ($outputs)" }

        val res = requestTargetMutex.withLock {
            captureRequestBuilder.addTarget(target)
        }
        setRepeatingSession()
        return res
    }

    /**
     * Removes targets from the current capture session
     *
     * @param targets The targets to remove
     */
    suspend fun removeTargets(targets: List<Surface>) {
        requestTargetMutex.withLock {
            targets.forEach {
                captureRequestBuilder.removeTarget(it)
            }
        }
        if (captureRequestBuilder.isEmpty) {
            stopRepeatingSession()
        } else {
            setRepeatingSession()
        }
    }

    /**
     * Removes a target from the current capture session
     *
     * @param target The target to remove
     */
    suspend fun removeTarget(target: Surface) {
        requestTargetMutex.withLock {
            captureRequestBuilder.removeTarget(target)
        }

        if (captureRequestBuilder.isEmpty) {
            stopRepeatingSession()
        } else {
            setRepeatingSession()
        }
    }

    fun release() {
        isReleased = true
        captureSession.close()
        runBlocking {
            isClosedFlow.first()
        }
    }

    /**
     * Sets a repeating session with the current capture request.
     */
    fun setRepeatingSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        if (isReleased) {
            Logger.w(TAG, "Camera session controller is released")
            return
        }
        if (captureRequestBuilder.isEmpty) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        captureSessionCompat.setRepeatingSingleRequest(
            captureSession, captureRequestBuilder.build(), cameraCaptureCallback
        )
    }

    fun setBurstSession(cameraCaptureCallback: CameraCaptureSession.CaptureCallback = captureCallback) {
        if (isReleased) {
            Logger.w(TAG, "Camera session controller is released")
            return
        }
        if (captureRequestBuilder.isEmpty) {
            Logger.w(TAG, "Capture request is empty")
            return
        }
        captureSessionCompat.captureBurstRequests(
            captureSession, listOf(captureRequestBuilder.build()), cameraCaptureCallback
        )
    }

    private fun stopRepeatingSession() {
        if (isReleased) {
            Logger.w(TAG, "Camera session controller is released")
            return
        }
        captureSession.stopRepeating()
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
    fun <T> setRepeatingSetting(key: CaptureRequest.Key<T>, value: T) {
        captureRequestBuilder.set(key, value)
        setRepeatingSession()
    }

    companion object {
        private const val TAG = "CameraSessionController"

        internal suspend fun create(
            sessionCompat: ICameraCaptureSessionCompat,
            manager: CameraManager,
            cameraDevice: CameraDeviceController,
            outputs: List<Surface>,
            dynamicRange: Long
        ): CameraSessionController {
            val isClosedFlow = MutableStateFlow(false)
            val captureSession =
                CameraUtils.createCaptureSession(
                    sessionCompat,
                    cameraDevice.camera,
                    outputs,
                    dynamicRange,
                    isClosedFlow
                )
            return CameraSessionController(
                manager,
                cameraDevice.id,
                sessionCompat,
                CameraCaptureSessionWithOutputs(captureSession, outputs),
                dynamicRange,
                isClosedFlow.asStateFlow()
            )
        }
    }
}


/**
 * A data class that holds a [CameraCaptureSession] and its outputs
 */
private data class CameraCaptureSessionWithOutputs(
    val session: CameraCaptureSession,
    val outputs: List<Surface>
)

/**
 * A builder for [CaptureRequest] with targets
 */
private class CaptureRequestBuilderWithTargets private constructor(
    private val captureRequest: CaptureRequest.Builder
) {
    private val mutableTargets = mutableSetOf<Surface>()

    /**
     * The targets of the CaptureRequest
     */
    val targets: List<Surface> get() = mutableTargets.toList()

    /**
     * Whether the CaptureRequest has no target
     */
    val isEmpty: Boolean
        get() = mutableTargets.isEmpty()

    /**
     * Whether the [CaptureRequest.Builder] has a target
     */
    fun hasTarget(surface: Surface) = mutableTargets.contains(surface)

    /**
     * Adds a target to the CaptureRequest
     *
     * @param surface The surface to add
     * @return true if the surface was added, false otherwise
     */
    fun addTarget(surface: Surface): Boolean {
        val wasAdded = mutableTargets.add(surface)
        if (wasAdded) {
            captureRequest.addTarget(surface)
        }
        return wasAdded
    }

    /**
     * Removes a target from the CaptureRequest
     *
     * @param surface The surface to remove
     * @return true if the surface was removed, false otherwise
     */
    fun removeTarget(surface: Surface): Boolean {
        val wasRemoved = mutableTargets.remove(surface)
        if (wasRemoved) {
            captureRequest.removeTarget(surface)
        }
        return wasRemoved
    }

    /**
     * Same as [CaptureRequest.Builder.set]
     */
    fun <T> set(key: CaptureRequest.Key<T>, value: T) = captureRequest.set(key, value)

    /**
     * Same as [CaptureRequest.Builder.get]
     */
    fun <T> get(key: CaptureRequest.Key<T?>) = captureRequest.get(key)

    /**
     * Same as [CaptureRequest.Builder.setTag]
     */
    fun setTag(tag: Any) = captureRequest.setTag(tag)

    /**
     * Same as [CaptureRequest.Builder.build]
     */
    fun build() = captureRequest.build()

    companion object {
        /**
         * Create a CaptureRequestBuilderWithTargets
         *
         * @param camera The camera device
         * @param template The template to use
         */
        fun create(
            camera: CameraDevice,
            template: Int = CameraDevice.TEMPLATE_RECORD,
        ) = CaptureRequestBuilderWithTargets(camera.createCaptureRequest(template))
    }
}