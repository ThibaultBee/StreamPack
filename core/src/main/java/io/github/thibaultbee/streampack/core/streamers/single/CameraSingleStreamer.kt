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
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.setPreview
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
     * Mutex to avoid concurrent access to preview surface.
     */
    private val previewMutex = Mutex()

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
         * It will block the current thread until the camera id is set. You can use [setCameraId] to
         * set it in a coroutine.
         *
         * @param value string that described the camera.
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            runBlocking {
                setCameraId(value)
            }
        }

    /**
     * Sets a camera id with a suspend function.
     *
     * @param cameraId The camera id to use
     */
    override suspend fun setCameraId(cameraId: String) {
        cameraSource.setCameraId(cameraId)
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
    override suspend fun setPreview(surface: Surface) {
        previewMutex.withLock {
            cameraSource.setPreviewSurface(surface)
        }
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
    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun startPreview() {
        previewMutex.withLock {
            cameraSource.startPreview()
        }
    }

    /**
     * Starts audio and video capture.
     * If you can prefer to call [ISingleStreamer.setAudioConfig] before starting preview.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param previewSurface The [Surface] used for camera preview
     *
     * @see [ICameraCoroutineStreamer.stopPreview]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun startPreview(previewSurface: Surface) {
        previewMutex.withLock {
            cameraSource.setPreviewSurface(previewSurface)
            cameraSource.startPreview()
        }
    }

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    override suspend fun stopPreview() {
        previewMutex.withLock {
            cameraSource.stopPreview()
        }
    }

    /**
     * Same as [SingleStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        runBlocking {
            stopPreview()
        }
        super.release()
    }
}