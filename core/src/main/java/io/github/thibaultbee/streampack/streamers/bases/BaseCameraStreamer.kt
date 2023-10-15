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
import io.github.thibaultbee.streampack.internal.data.orientation.DeviceOrientationProvider
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.sources.AudioCapture
import io.github.thibaultbee.streampack.internal.sources.camera.CameraCapture
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.helpers.CameraStreamerConfigurationHelper
import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.streamers.settings.BaseCameraStreamerSettings
import io.github.thibaultbee.streampack.views.AutoFitSurfaceView
import kotlinx.coroutines.runBlocking

/**
 * A [BaseStreamer] that sends microphone and camera frames.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to capture audio
 * @param muxer a [IMuxer] implementation
 * @param endpoint a [IEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
open class BaseCameraStreamer(
    private val context: Context,
    enableAudio: Boolean = true,
    muxer: IMuxer,
    endpoint: IEndpoint,
    initialOnErrorListener: OnErrorListener? = null
) : BaseStreamer(
    context = context,
    videoCapture = CameraCapture(context),
    audioCapture = if (enableAudio) AudioCapture() else null,
    orientationProvider = DeviceOrientationProvider(context),
    muxer = muxer,
    endpoint = endpoint,
    initialOnErrorListener = initialOnErrorListener
), ICameraStreamer {
    private val cameraCapture = videoCapture as CameraCapture
    override val helper = CameraStreamerConfigurationHelper(muxer.helper)

    /**
     * Get/Set current camera id.
     */
    override var camera: String
        /**
         * Get current camera id.
         *
         * @return a string that described current camera
         */
        get() = cameraCapture.cameraId
        /**
         * Set current camera id.
         *
         * @param value string that described the camera. Retrieves list of camera from [Context.cameraList]
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            cameraCapture.cameraId = value
        }

    override var settings =
        BaseCameraStreamerSettings(audioCapture, cameraCapture, audioEncoder, videoEncoder)

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * Inside, it launches both camera and microphone capture.
     *
     * @param previewSurface Where to display camera capture. Could be a [Surface] from a [StreamerSurfaceView], an [AutoFitSurfaceView], a [SurfaceView] or a [TextureView].
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
                cameraCapture.previewSurface = previewSurface
                cameraCapture.encoderSurface = videoEncoder?.inputSurface
                cameraCapture.startPreview(cameraId)
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
        stopStream()
        cameraCapture.stopPreview()
    }

    /**
     * Same as [BaseStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}