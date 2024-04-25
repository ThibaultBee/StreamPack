/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.streamers.bases

import android.Manifest
import android.content.Context
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.sources.audio.MicrophoneSource
import io.github.thibaultbee.streampack.internal.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.internal.sources.video.camera.IPublicCameraSource
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.helpers.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import kotlinx.coroutines.runBlocking

/**
 * A [BaseStreamer] that sends microphone and camera frames.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
open class BaseCameraStreamer(
    private val context: Context,
    enableAudio: Boolean = true,
    internalEndpoint: IEndpoint,
    initialOnErrorListener: OnErrorListener? = null
) : BaseStreamer(
    context = context,
    internalVideoSource = CameraSource(context),
    internalAudioSource = if (enableAudio) MicrophoneSource() else null,
    internalEndpoint = internalEndpoint,
    initialOnErrorListener = initialOnErrorListener
), ICameraStreamer {
    private val internalCameraSource = internalVideoSource as CameraSource

    override val info = CameraStreamerConfigurationInfo(internalEndpoint.info)

    /**
     * Gets the camera source.
     * It allows to configure camera settings and to set the camera id.
     */
    override val videoSource: IPublicCameraSource
        get() = internalCameraSource

    /**
     * Get/Set current camera id.
     * It is a shortcut for [videoSource.cameraId]
     */
    override var camera: String
        /**
         * Get current camera id.
         *
         * @return a string that described current camera
         */
        get() = videoSource.cameraId
        /**
         * Set current camera id.
         *
         * @param value string that described the camera. Retrieves list of camera from [Context.cameraList]
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            videoSource.cameraId = value
        }

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * Inside, it launches both camera and microphone capture.
     *
     * @param previewSurface Where to display camera capture. Could be a [Surface] from a [SurfaceView] or a [TextureView].
     * @param cameraId camera id (get camera id list from [Context.cameraList])
     *
     * @throws [StreamPackError] if audio or video capture couldn't be launch
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    override fun startPreview(previewSurface: Surface, cameraId: String) {
        require(videoConfig != null) { "Video has not been configured!" }
        runBlocking {
            try {
                internalCameraSource.previewSurface = previewSurface
                internalCameraSource.encoderSurface = codecSurface?.input
                internalCameraSource.startPreview(cameraId)
            } catch (e: Exception) {
                stopPreview()
                throw StreamPackError(e)
            }
        }
    }

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    override fun stopPreview() {
        runBlocking {
            stopStream()
        }
        internalCameraSource.stopPreview()
    }

    /**
     * Same as [BaseStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}