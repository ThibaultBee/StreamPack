/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamers.single.callbacks

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.setPreview
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import io.github.thibaultbee.streampack.core.streamers.single.CameraSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICallbackSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.ICoroutineSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.open
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default implementation of [ICallbackSingleStreamer] that uses [ICoroutineSingleStreamer] to handle streamer logic.
 * It is a bridge between [ICoroutineSingleStreamer] and [ICallbackSingleStreamer].
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpointInternal] implementation
 */
class CameraCallbackSingleStreamer(
    private val context: Context,
    enableMicrophone: Boolean = true,
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context)
) : CallbackSingleStreamer(CameraSingleStreamer(context, enableMicrophone, internalEndpoint)),
    ICameraCallbackStreamer {
    private val cameraSource = (streamer as CameraSingleStreamer).videoSource as CameraSource

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
         *
         * @param value string that described the camera. Retrieves list of camera from [Context.cameras]
         */
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            runBlocking {
                cameraSource.setCameraId(value)
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

    /**
     * Sets a preview surface.
     */
    override fun setPreview(surface: Surface) {
        runBlocking {
            previewMutex.withLock { cameraSource.setPreviewSurface(surface) }
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
    override fun startPreview() {
        /**
         * Trying to set encoder surface to avoid a camera restart.
         */
        coroutineScope.launch {
            previewMutex.withLock {
                try {
                    cameraSource.startPreview()
                } catch (t: Throwable) {
                    listeners.forEach { it.onError(t) }
                }
            }
        }
    }

    /**
     * Starts audio and video capture.
     * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param previewSurface The [Surface] used for camera preview
     *
     * @see [ICameraCoroutineStreamer.stopPreview]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    override fun startPreview(previewSurface: Surface) {
        /**
         * Trying to set encoder surface to avoid a camera restart.
         */
        coroutineScope.launch {
            previewMutex.withLock {
                try {
                    cameraSource.setPreviewSurface(previewSurface)
                    cameraSource.startPreview()
                } catch (t: Throwable) {
                    listeners.forEach { it.onError(t) }
                }
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
            previewMutex.withLock {
                cameraSource.stopPreview()
            }
        }
    }

    /**
     * Same as [CallbackSingleStreamer.release] but it also calls [stopPreview].
     */
    override fun release() {
        stopPreview()
        super.release()
    }
}