package com.github.thibaultbee.streampack.sources

import android.Manifest
import android.content.Context
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.interfaces.Controllable
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.utils.Error
import com.github.thibaultbee.streampack.utils.EventHandlerManager
import com.github.thibaultbee.streampack.utils.Logger
import com.github.thibaultbee.streampack.utils.getFpsList
import java.security.InvalidParameterException


class CameraCapture(
    private val context: Context,
    override var onErrorListener: OnErrorListener?,
    val logger: Logger
) : EventHandlerManager(), Controllable {
    var fpsRange = Range(0, 30)

    lateinit var previewSurface: Surface
    lateinit var encoderSurface: Surface

    var cameraId: String = "0"

    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            logger.e(this, "Camera Session configuration failed")
            session.close()
            reportError(Error.INVALID_OPERATION)
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            captureRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder?.let {
                it.addTarget(previewSurface)
                it.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                captureSession?.setRepeatingRequest(
                    it.build(),
                    null,
                    null
                )
            }
        }

        override fun onClosed(session: CameraCaptureSession) {
            captureRequestBuilder = null
            captureSession = null
        }
    }

    private val cameraDeviceCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@CameraCapture.camera = camera
            cameraId = camera.id
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            logger.e(this, "Camera ${camera.id} is in error $error")
            reportError(
                when (error) {
                    ERROR_CAMERA_IN_USE -> Error.DEVICE_ALREADY_IN_USE
                    ERROR_MAX_CAMERAS_IN_USE -> Error.DEVICE_MAX_IN_USE
                    ERROR_CAMERA_DISABLED -> Error.DEVICE_DISABLED
                    ERROR_CAMERA_DEVICE -> Error.UNKNOWN
                    ERROR_CAMERA_SERVICE -> Error.UNKNOWN
                    else -> Error.UNKNOWN
                }
            )
            this@CameraCapture.camera = null
        }

        override fun onClosed(camera: CameraDevice) {
            this@CameraCapture.camera = null
            logger.d(this, "Camera ${camera.id} is closed")
        }
    }

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private fun createCaptureSession() {
        val surfaceList = mutableListOf(previewSurface, encoderSurface)
        camera?.createCaptureSession(surfaceList, captureSessionCallback, backgroundHandler)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun getClosestFpsRange(fps: Int): Range<Int> {
        var fpsRangeList = context.getFpsList(cameraId)
        // Get range that contains FPS
        fpsRangeList =
            fpsRangeList.filter { it.contains(fps) or it.contains(fps * 1000) } // On Samsung S4 fps range is [4000-30000] instead of [4-30]
        if (fpsRangeList.isEmpty()) {
            throw InvalidParameterException("Failed to find a single FPS range that contains $fps")
        }

        // Get smaller range
        var selectedFpsRange = fpsRangeList[0]
        fpsRangeList = fpsRangeList.drop(0)
        fpsRangeList.forEach {
            if ((it.upper - it.lower) < (selectedFpsRange.upper - selectedFpsRange.lower)) {
                selectedFpsRange = it
            }
        }

        return selectedFpsRange
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun configure(fps: Int) {
        fpsRange = getClosestFpsRange(fps)

        startBackgroundThread()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview(cameraId: String) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler)
    }

    fun stopPreview() {
        captureRequestBuilder = null

        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null
    }

    override fun startStream() {
        captureRequestBuilder?.let {
            it.addTarget(encoderSurface)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                null
            )
        } ?: throw IllegalStateException("Camera is not ready for stream")
    }

    override fun stopStream() {
        captureRequestBuilder?.let {
            it.removeTarget(encoderSurface)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                null
            )
        }
    }

    override fun release() {
        stopBackgroundThread()
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("BackgroundCamera").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}