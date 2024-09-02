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
package io.github.thibaultbee.streampack.core.streamers

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.internal.sources.audio.MicrophoneSource
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.IPublicCameraSource
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer

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
    private val cameraSource = internalVideoSource as CameraSource

    /**
     * Gets the camera source.
     * It allows to configure camera settings and to set the camera id.
     */
    override val videoSource: IPublicCameraSource
        get() = cameraSource

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
     * Sets a preview surface.
     */
    override fun setPreview(surface: Surface) {
        cameraSource.previewSurface = surface
    }

    /**
     * Starts video preview.
     *
     * The preview will be rendered on the surface set by [setPreview].
     * It is recommend to call configure before call [startPreview] to avoid camera restart when
     * encoder surface will be added.
     *
     * @see [stopPreview]
     * @see [setPreview]
     */
    override suspend fun startPreview() {
        /**
         * Trying to set encoder surface to avoid a camera restart.
         */
        cameraSource.outputSurface = codecSurface?.input
        cameraSource.startPreview()
    }

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    override fun stopPreview() {
        cameraSource.stopPreview()
    }

    /**
     * Same as [DefaultStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}