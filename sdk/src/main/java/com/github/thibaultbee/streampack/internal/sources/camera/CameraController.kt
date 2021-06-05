package com.github.thibaultbee.streampack.internal.sources.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.*
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.error.CameraError
import com.github.thibaultbee.streampack.utils.ILogger
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraController(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: ILogger
) {
    var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var captureRequest: CaptureRequest.Builder? = null

    private val threadManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        CameraExecutorManager()
    } else {
        CameraHandlerManager()
    }

    private class CameraDeviceCallback(
        private val cont: CancellableContinuation<CameraDevice>,
        private val logger: ILogger
    ) : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) = cont.resume(device)

        override fun onDisconnected(camera: CameraDevice) {
            logger.w(this, "Camera ${camera.id} has been disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            logger.e(this, "Camera ${camera.id} is in error $error")

            val exc = when (error) {
                ERROR_CAMERA_IN_USE -> CameraError("Camera already in use")
                ERROR_MAX_CAMERAS_IN_USE -> CameraError("Max cameras in use")
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
        private val logger: ILogger
    ) : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.e(this, "Camera Session configuration failed")
            cont.resumeWithException(CameraError("Camera: failed to configure the capture session"))
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
            logger.e(this, "Capture failed  with code ${failure.reason}")
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        threadManager.openCamera(
            manager,
            cameraId,
            CameraDeviceCallback(cont, logger)
        )
    }

    private suspend fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        threadManager.createCaptureSession(
            camera,
            targets,
            CameraCaptureSessionCallback(cont, logger)
        )
    }

    private fun createRequestSession(
        camera: CameraDevice,
        captureSession: CameraCaptureSession,
        fpsRange: Range<Int>,
        surfaces: List<Surface>
    ): CaptureRequest.Builder {
        if (surfaces.isEmpty()) {
            throw RuntimeException("No target surface")
        }

        return camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            surfaces.forEach { addTarget(it) }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            threadManager.setRepeatingRequest(captureSession, build(), captureCallback)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startCamera(
        cameraId: String,
        targets: List<Surface>
    ) {
        require(targets.isNotEmpty()) { " At least one target is required" }

        withContext(coroutineDispatcher) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            camera = openCamera(manager, cameraId).also { cameraDevice ->
                captureSession =
                    createCaptureSession(
                        cameraDevice,
                        targets
                    )
            }
        }
    }

    fun startRequestSession(fpsRange: Range<Int>, targets: List<Surface>) {
        require(camera != null) { "Camera must not be null" }
        require(captureSession != null) { "Capture session must not be null" }
        require(targets.isNotEmpty()) { " At least one target is required" }

        captureRequest =
            createRequestSession(
                camera!!,
                captureSession!!,
                fpsRange,
                targets
            )
    }

    fun stopCamera() {
        captureRequest = null

        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null
    }

    fun addTargets(targets: List<Surface>) {
        require(captureRequest != null) { "capture request must not be null" }
        require(targets.isNotEmpty()) { " At least one target is required" }

        targets.forEach {
            captureRequest!!.addTarget(it)
        }
        updateCaptureSession()
    }

    fun addTarget(target: Surface) {
        require(captureRequest != null) { "capture request must not be null" }

        captureRequest!!.addTarget(target)

        updateCaptureSession()
    }

    fun removeTarget(target: Surface) {
        require(captureRequest != null) { "capture request must not be null" }

        captureRequest!!.removeTarget(target)
        updateCaptureSession()
    }

    fun release() {
        threadManager.release()
    }

    private fun updateCaptureSession() {
        require(captureSession != null) { "capture session must not be null" }
        require(captureRequest != null) { "capture request must not be null" }

        threadManager.setRepeatingRequest(
            captureSession!!,
            captureRequest!!.build(),
            captureCallback
        )
    }
}