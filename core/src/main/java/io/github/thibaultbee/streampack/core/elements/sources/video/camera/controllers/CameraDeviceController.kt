package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.sessioncompat.ICameraCaptureSessionCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable

/**
 * A class that encapsulates a [CameraDevice] and implements [Closeable].
 *
 * @param cameraDevice The [CameraDevice] to be controlled.
 * @param sessionCompat The [ICameraCaptureSessionCompat] to be used for creating capture sessions.
 * @param isClosedFlow A [StateFlow] that indicates whether the camera device is closed.
 */
internal class CameraDeviceController private constructor(
    private val cameraDevice: CameraDevice,
    private val sessionCompat: ICameraCaptureSessionCompat,
    val isClosedFlow: StateFlow<Boolean>
) :
    Closeable {
    private val mutex = Mutex()

    val isClosed: Boolean
        get() = isClosedFlow.value

    val id = cameraDevice.id

    private var cameraAudioRestriction: Int
        @RequiresApi(Build.VERSION_CODES.R)
        get() = cameraDevice.cameraAudioRestriction
        @RequiresApi(Build.VERSION_CODES.R)
        set(value) {
            cameraDevice.cameraAudioRestriction = value
        }

    fun createCaptureRequest(
        templateType: Int
    ): CaptureRequest.Builder = runBlocking {
        mutex.withLock {
            if (isClosed) {
                throw IllegalStateException("Camera $id is closed")
            }
        }
        return@runBlocking cameraDevice.createCaptureRequest(templateType)
    }

    fun createCaptureSession(
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    ) = runBlocking {
        mutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Camera $id is closed")
                return@runBlocking
            }
            sessionCompat.createCaptureSession(cameraDevice, targets, callback)
        }
    }


    fun createCaptureSessionByOutputConfiguration(
        outputConfigurations: List<OutputConfiguration>,
        callback: CameraCaptureSession.StateCallback
    ) = runBlocking {
        mutex.withLock {
            if (isClosed) {
                Logger.w(TAG, "Camera $id is closed")
                return@runBlocking
            }
            sessionCompat.createCaptureSessionByOutputConfiguration(
                cameraDevice,
                outputConfigurations,
                callback
            )
        }
    }

    fun muteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                cameraDevice.cameraAudioRestriction =
                    CameraDevice.AUDIO_RESTRICTION_VIBRATION_SOUND
            } catch (t: Throwable) {
                Logger.w(TAG, "Failed to mute vibration and sound $t")
            }
        }
    }

    fun unmuteVibrationAndSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                cameraDevice.cameraAudioRestriction = CameraDevice.AUDIO_RESTRICTION_NONE
            } catch (t: Throwable) {
                Logger.w(TAG, "Failed to unmute vibration and sound: $t")
            }
        }
    }

    override fun close() {
        runBlocking {
            mutex.withLock {
                if (isClosed) {
                    Logger.w(TAG, "Camera $id already closed")
                    return@withLock
                }
                cameraDevice.close()
                if (!isClosedFlow.value) {
                    runBlocking {
                        isClosedFlow.first { it }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CameraDeviceController"

        @RequiresPermission(Manifest.permission.CAMERA)
        suspend fun create(
            manager: CameraManager,
            sessionCompat: ICameraCaptureSessionCompat,
            cameraId: String,
        ): CameraDeviceController {
            val isClosedFlow = MutableStateFlow(false)

            val cameraDevice = CameraUtils.openCamera(
                sessionCompat,
                manager,
                cameraId,
                isClosedFlow
            )

            return CameraDeviceController(
                cameraDevice,
                sessionCompat,
                isClosedFlow
            )
        }
    }
}