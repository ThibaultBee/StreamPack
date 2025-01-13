package io.github.thibaultbee.streampack.core.elements.sources.video.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.dispatchers.ICameraDispatcher
import io.github.thibaultbee.streampack.core.elements.utils.extensions.resumeWithExceptionIfActive
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal object CameraUtils {
    private const val TAG = "CameraUtils"

    @RequiresPermission(Manifest.permission.CAMERA)
    internal suspend fun openCamera(
        cameraDispatcher: ICameraDispatcher, manager: CameraManager, cameraId: String
    ): CameraDevice = suspendCancellableCoroutine { continuation ->
        val callbacks = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resume(device)

            override fun onDisconnected(camera: CameraDevice) {
                Logger.w(TAG, "Camera ${camera.id} has been disconnected")
                continuation.resumeWithExceptionIfActive(RuntimeException("Camera has been disconnected"))
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Logger.e(TAG, "Camera ${camera.id} is in error $error")

                val exc = when (error) {
                    ERROR_CAMERA_IN_USE -> CameraException("Camera already in use")
                    ERROR_MAX_CAMERAS_IN_USE -> CameraException("Max cameras in use")

                    ERROR_CAMERA_DISABLED -> CameraException("Camera has been disabled")
                    ERROR_CAMERA_DEVICE -> CameraException("Camera device has crashed")
                    ERROR_CAMERA_SERVICE -> CameraException("Camera service has crashed")
                    else -> CameraException("Unknown error")
                }
                continuation.resumeWithExceptionIfActive(exc)
            }
        }
        cameraDispatcher.openCamera(manager, cameraId, callbacks)
    }

    internal suspend fun createCaptureSession(
        cameraDispatcher: ICameraDispatcher,
        camera: CameraDevice,
        targets: List<Surface>,
        dynamicRange: Long,
    ): CameraCaptureSession = suspendCancellableCoroutine { continuation ->
        val callbacks = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = continuation.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Logger.e(TAG, "Camera session configuration failed")
                continuation.resumeWithExceptionIfActive(CameraException("Camera: failed to configure the capture session"))
            }

            override fun onClosed(session: CameraCaptureSession) {
                Logger.e(TAG, "Camera capture session closed")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val outputConfigurations = targets.map {
                OutputConfiguration(it).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        dynamicRangeProfile = dynamicRange
                    }
                }
            }

            cameraDispatcher.createCaptureSessionByOutputConfiguration(
                camera, outputConfigurations, callbacks
            )
        } else {
            cameraDispatcher.createCaptureSession(
                camera, targets, callbacks
            )
        }
    }

    internal suspend fun createRequestSession(
        cameraDispatcher: ICameraDispatcher,
        context: Context,
        camera: CameraDevice,
        captureSession: CameraCaptureSession,
        fpsRange: Range<Int>,
        surfaces: List<Surface>
    ): CaptureRequest.Builder = suspendCancellableCoroutine { continuation ->
        val callbacks = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {

            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                continuation.resumeWithExceptionIfActive(RuntimeException("capture failed"))
            }
        }
        camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            surfaces.forEach { addTarget(it) }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            if (context.getAutoFocusModes(camera.id)
                    .contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            ) {
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }
            cameraDispatcher.setRepeatingSingleRequest(
                captureSession, build(), callbacks
            )
            continuation.resume(this)
        }
    }

    internal fun getClosestFpsRange(context: Context, cameraId: String, fps: Int): Range<Int> {
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
}