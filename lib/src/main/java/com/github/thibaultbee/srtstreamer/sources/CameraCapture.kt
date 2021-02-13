package com.github.thibaultbee.srtstreamer.sources

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.srtstreamer.utils.Error
import com.github.thibaultbee.srtstreamer.utils.EventHandlerManager
import com.github.thibaultbee.srtstreamer.utils.Logger


class CameraCapture(private val context: Context, val logger: Logger) :
    EventHandlerManager() {
    var fpsRange = Range(30, 30)

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
        camera?.createCaptureSession(surfaceList, captureSessionCallback, null)
    }

    fun isRunning() = camera != null


    @RequiresPermission(Manifest.permission.CAMERA)
    fun getClosestFpsRange(fps: Int): Range<Int>? {
        var fpsRangeList = getFpsList(cameraId)
        // Get range that contains FPS
        fpsRangeList =
            fpsRangeList.filter { it.contains(fps) or it.contains(fps * 1000) } // On Samsung S4 fps range is [4000-30000] instead of [4-30]
        if (fpsRangeList.isEmpty()) {
            logger.e(this, "Failed to find a single FPS range that contains $fps")
            return null
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
    fun configure(fps: Int): Error {
        val selectedFpsRange = getClosestFpsRange(fps)
        if (selectedFpsRange == null) {
            logger.e(this, "Failed to get supported FPS range")
            return Error.INVALID_PARAMETER
        }
        fpsRange = selectedFpsRange

        captureRequestBuilder?.let {
            it.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                null
            )
        }

        return Error.SUCCESS
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview(cameraId: String): Error {
        startBackgroundThread()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler)

        return Error.SUCCESS
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview(): Error {
        startBackgroundThread()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId, cameraDeviceCallback, null)

        return Error.SUCCESS
    }

    fun stopPreview(): Error {
        captureRequestBuilder = null

        captureSession?.close()
        captureSession = null

        camera?.close()
        camera = null
        stopBackgroundThread()

        return Error.SUCCESS
    }

    fun startStream(): Error {
        captureRequestBuilder?.let {
            it.addTarget(encoderSurface)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                null
            )
        }
        return Error.SUCCESS
    }

    fun stopStream(): Error {
        captureRequestBuilder?.let {
            it.removeTarget(encoderSurface)
            captureSession?.setRepeatingRequest(
                it.build(),
                null,
                null
            )
        }
        return Error.SUCCESS
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

    @RequiresPermission(Manifest.permission.CAMERA)
    fun getCameraCharacteristics(cameraId: String? = null): CameraCharacteristics? {
        val id = cameraId ?: camera?.id ?: return null

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.getCameraCharacteristics(id)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun getCameraList(): List<String> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.cameraIdList.toList()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun getOutputCaptureSizes(cameraId: String? = null): List<Size> {
        val id = cameraId ?: camera?.id ?: return emptyList()

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.getCameraCharacteristics(id)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
            ImageFormat.YUV_420_888
        )?.toList() ?: emptyList()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun <T : Any> getOutputSizes(klass: Class<T>, cameraId: String? = null): List<Size> {
        val id = cameraId ?: camera?.id ?: return emptyList()

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.getCameraCharacteristics(id)[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(
            klass
        )?.toList() ?: emptyList()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun getFpsList(cameraId: String? = null): List<Range<Int>> {
        val id = cameraId ?: camera?.id ?: return emptyList()

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.getCameraCharacteristics(id)[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
            ?: emptyList()
    }
}