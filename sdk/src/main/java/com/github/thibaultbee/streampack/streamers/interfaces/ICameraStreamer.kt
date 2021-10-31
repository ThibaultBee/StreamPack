package com.github.thibaultbee.streampack.streamers.interfaces

import android.Manifest
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import com.github.thibaultbee.streampack.utils.CameraSettings

interface ICameraStreamer {
    /**
     * Get/Set current camera id.
     */
    var camera: String

    /**
     * Get the camera settings.
     */
    val cameraSettings: CameraSettings

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface, cameraId: String = "0")

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    fun stopPreview()
}