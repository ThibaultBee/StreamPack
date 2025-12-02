package io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Gets the camera manager.
 */
val Context.cameraManager: CameraManager
    get() = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

/**
 * Gets camera characteristics.
 *
 * @param cameraId camera id
 * @return camera characteristics
 */
fun Context.getCameraCharacteristics(cameraId: String): CameraCharacteristics {
    return cameraManager.getCameraCharacteristics(cameraId)
}

/**
 * Gets the default camera id.
 */
val Context.defaultCameraId: String
    get() = cameraManager.defaultCameraId