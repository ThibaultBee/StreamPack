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
package com.github.thibaultbee.streampack.streamers.bases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.github.thibaultbee.streampack.error.StreamPackError
import com.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.sources.AudioCapture
import com.github.thibaultbee.streampack.internal.sources.camera.CameraCapture
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.streamers.CameraSrtLiveStreamer
import com.github.thibaultbee.streampack.streamers.CameraTsFileStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import com.github.thibaultbee.streampack.utils.CameraSettings
import com.github.thibaultbee.streampack.utils.getCameraList
import kotlinx.coroutines.runBlocking

/**
 * Base class of camera streamer: [CameraTsFileStreamer] or [CameraSrtLiveStreamer]
 * Use this class, only if you want to implement a custom endpoint with a camera source.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param endpoint a [IEndpoint] implementation
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio
 */
open class BaseCameraStreamer(
    private val context: Context,
    tsServiceInfo: ServiceInfo,
    endpoint: IEndpoint,
    logger: ILogger,
    enableAudio: Boolean
) : BaseStreamer(
    context = context,
    tsServiceInfo = tsServiceInfo,
    videoCapture = CameraCapture(context, logger = logger),
    audioCapture = if (enableAudio) AudioCapture(logger) else null,
    endpoint = endpoint,
    logger = logger
), ICameraStreamer {
    private val cameraCapture = videoCapture as CameraCapture

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
         * @param value string that described the camera. Retrieves list of camera from [Context.getCameraList]
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            cameraCapture.let { it.cameraId = value }
        }

    /**
     * Get the camera settings.
     */
    override val cameraSettings: CameraSettings
        get() = cameraCapture.settings

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * Inside, it launches both camera and microphone capture.
     *
     * @param previewSurface Where to display camera capture
     * @param cameraId camera id (get camera id list from [Context.getCameraList])
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
        stopStreamImpl()
        cameraCapture.stopPreview()
    }

    /**
     * Stops camera and ask for a reset if camera is already running.
     */
    override fun onResetVideo(): Boolean {
        val restartPreview = cameraCapture.isPreviewing
        cameraCapture.stopPreview()
        return restartPreview
    }

    /**
     * Start preview when video system has been reset.
     */
    override suspend fun afterResetVideo() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraCapture.startPreview()
        }
    }

    /**
     * Same as [BaseStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}