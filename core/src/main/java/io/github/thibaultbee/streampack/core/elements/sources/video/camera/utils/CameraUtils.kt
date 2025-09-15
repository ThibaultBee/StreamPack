package io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraException
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraDeviceController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getCameraFps
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.utils.extensions.resumeIfActive
import io.github.thibaultbee.streampack.core.elements.utils.extensions.resumeWithExceptionIfActive
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object CameraUtils {
    private const val TAG = "CameraUtils"

    @RequiresPermission(Manifest.permission.CAMERA)
    internal suspend fun openCamera(
        sessionCompat: ICameraCaptureSessionCompat,
        manager: CameraManager,
        cameraId: String,
        isClosedFlow: MutableStateFlow<Boolean>
    ): CameraDevice = suspendCancellableCoroutine { continuation ->
        val callbacks = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resumeIfActive(device)

            override fun onDisconnected(camera: CameraDevice) {
                isClosedFlow.tryEmit(true)
                Logger.w(TAG, "Camera ${camera.id} has been disconnected")
                continuation.resumeWithExceptionIfActive(RuntimeException("Camera has been disconnected"))
            }

            override fun onError(camera: CameraDevice, error: Int) {
                isClosedFlow.tryEmit(true)
                Logger.e(TAG, "Camera ${camera.id} is in error $error")

                val exception = when (error) {
                    ERROR_CAMERA_IN_USE -> CameraException("Camera already in use")
                    ERROR_MAX_CAMERAS_IN_USE -> CameraException("Max cameras in use")

                    ERROR_CAMERA_DISABLED -> CameraException("Camera has been disabled")
                    ERROR_CAMERA_DEVICE -> CameraException("Camera device has crashed")
                    ERROR_CAMERA_SERVICE -> CameraException("Camera service has crashed")
                    else -> CameraException("Unknown error")
                }
                continuation.resumeWithException(exception)
            }

            override fun onClosed(camera: CameraDevice) {
                isClosedFlow.tryEmit(true)
                Logger.i(TAG, "Camera ${camera.id} has been closed")
            }
        }
        sessionCompat.openCamera(manager, cameraId, callbacks)
    }

    internal suspend fun createCaptureSession(
        cameraDeviceController: CameraDeviceController,
        outputs: List<Surface>,
        dynamicRange: Long,
        isClosedFlow: MutableStateFlow<Boolean>
    ): CameraCaptureSession = suspendCancellableCoroutine { continuation ->
        val cameraId = cameraDeviceController.id
        val callbacks = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) =
                continuation.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                isClosedFlow.tryEmit(true)
                Logger.e(
                    TAG,
                    "Camera session configuration failed for camera $cameraId and outputs $outputs"
                )
                continuation.resumeWithExceptionIfActive(CameraException("Camera: failed to configure the capture session for camera $cameraId and outputs $outputs"))
            }

            override fun onClosed(session: CameraCaptureSession) {
                isClosedFlow.tryEmit(true)
                Logger.e(
                    TAG,
                    "Camera capture session closed for camera $cameraId and outputs $outputs"
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val outputConfigurations = outputs.map {
                OutputConfiguration(it).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        dynamicRangeProfile = dynamicRange
                    }
                }
            }

            cameraDeviceController.createCaptureSessionByOutputConfiguration(
                outputConfigurations, callbacks
            )
        } else {
            cameraDeviceController.createCaptureSession(
                outputs, callbacks
            )
        }
    }

    internal fun getClosestFpsRange(
        cameraManager: CameraManager,
        cameraId: String,
        fps: Int
    ): Range<Int> {
        var fpsRangeList = cameraManager.getCameraFps(cameraId)
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