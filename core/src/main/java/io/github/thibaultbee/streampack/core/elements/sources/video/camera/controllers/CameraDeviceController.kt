package io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers

import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Closeable

/**
 * A data class that encapsulates a [CameraDevice] and implements [Closeable].
 *
 * Avoid to call [CameraDevice.close] multiple times.
 */
data class CameraDeviceController(val camera: CameraDevice) : Closeable {
    private var isClosed: Boolean = false

    val id = camera.id

    var cameraAudioRestriction: Int
        @RequiresApi(Build.VERSION_CODES.R)
        get() = camera.cameraAudioRestriction
        @RequiresApi(Build.VERSION_CODES.R)
        set(value) {
            camera.cameraAudioRestriction = value
        }

    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
        camera.close()
    }
}