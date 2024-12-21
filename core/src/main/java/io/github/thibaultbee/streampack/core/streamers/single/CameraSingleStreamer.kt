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
package io.github.thibaultbee.streampack.core.streamers.single

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo.*
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.internal.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.internal.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.internal.utils.RotationValue
import io.github.thibaultbee.streampack.core.internal.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer

/**
 * A [SingleStreamer] that sends microphone and camera frames.
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpointInternal] implementation
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
fun CameraSingleStreamer(
    context: Context,
    enableMicrophone: Boolean = true,
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context),
    @RotationValue defaultRotation: Int = context.displayRotation
) = CameraSingleStreamer(
    context,
    if (enableMicrophone) buildDefaultMicrophoneSource() else null,
    internalEndpoint,
    defaultRotation
)

/**
 * A [SingleStreamer] that sends from camera frames and [audioSourceInternal] audio frames.
 *
 * @param context application context
 * @param audioSourceInternal the audio source implementation
 * @param internalEndpoint the [IEndpointInternal] implementation
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class CameraSingleStreamer(
    context: Context,
    audioSourceInternal: IAudioSourceInternal?,
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context),
    @RotationValue defaultRotation: Int = context.displayRotation
) : SingleStreamer(
    context = context,
    audioSourceInternal = audioSourceInternal,
    videoSourceInternal = CameraSource(context),
    endpointInternal = internalEndpoint,
    defaultRotation = defaultRotation
), ICameraCoroutineStreamer {
    private val cameraSource = videoSourceInternal as CameraSource

    /**
     * Gets the camera source.
     * It allows to configure camera settings and to set the camera id.
     */
    override val videoSource = cameraSource as ICameraSource

    /**
     * Get/Set current camera id.
     * It is a shortcut for [CameraSource.cameraId]
     */
    override var cameraId: String
        /**
         * Get current camera id.
         *
         * @return a string that described current camera
         */
        get() = videoSource.cameraId
        /**
         * Set current camera id.
         * Retrieves list of cameras from [Context.cameras]
         *
         * @param value string that described the camera.
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            videoSource.cameraId = value
            // If config has not been set yet, [configure] will update transformation later.
            if (videoConfig != null) {
                updateTransformation()
            }
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

    override fun isMirroringRequired(): Boolean {
        return cameraSource.infoProvider.isFrontFacing
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
     * Same as [SingleStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}