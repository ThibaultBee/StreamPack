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
package io.github.thibaultbee.streampack.streamers

import android.Manifest
import android.content.Context
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.sources.audio.MicrophoneSource
import io.github.thibaultbee.streampack.internal.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.internal.sources.video.camera.IPublicCameraSource
import io.github.thibaultbee.streampack.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import kotlinx.coroutines.runBlocking

/**
 * A [DefaultStreamer] that sends microphone and camera frames.
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpoint] implementation
 */
open class DefaultCameraStreamer(
    private val context: Context,
    enableMicrophone: Boolean = true,
    internalEndpoint: IEndpoint = DynamicEndpoint(context)
) : DefaultStreamer(
    context = context,
    internalVideoSource = CameraSource(context),
    internalAudioSource = if (enableMicrophone) MicrophoneSource() else null,
    internalEndpoint = internalEndpoint
), ICameraStreamer {
    private val internalCameraSource = internalVideoSource as CameraSource

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
     * Gets configuration information.
     *
     * When using the [DynamicEndpoint], the endpoint type is unknown until [open] is called.
     * In this case, prefer using [getInfo] with the [MediaDescriptor] used in [open].
     */
    override val info: IConfigurationInfo
        get() = CameraStreamerConfigurationInfo(endpoint.info)

    /**
     * Gets configuration information from [MediaDescriptor].
     *
     * @param descriptor the media descriptor
     */
    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        val endpointInfo = if (endpoint is DynamicEndpoint) {
            (endpoint as DynamicEndpoint).getInfo(descriptor)
        } else {
            endpoint.info
        }
        return CameraStreamerConfigurationInfo(endpointInfo)
    }

    /**
     * Starts audio and video capture.
     * [DefaultStreamer.configure] must have been called at least once.
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
     * Same as [DefaultStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}