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
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.setPreview
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import kotlinx.coroutines.runBlocking

/**
 * Creates a [CameraSingleStreamer] with a default audio source.
 *
 * @param context the application context
 * @param audioSourceInternal the audio source implementation. By default, it is the default microphone source.
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraSingleStreamer(
    context: Context,
    audioSourceInternal: IAudioSourceInternal = buildDefaultMicrophoneSource(),
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): CameraSingleStreamer {
    val streamer = CameraSingleStreamer(
        context, true, endpointInternalFactory, defaultRotation
    )
    streamer.setAudioSource(audioSourceInternal)
    return streamer
}

/**
 * Creates a [CameraSingleStreamer].
 *
 * @param context the application context
 * @param hasAudio [Boolean.true] if the streamer will capture audio.
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraSingleStreamer(
    context: Context,
    hasAudio: Boolean,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): CameraSingleStreamer {
    val source = CameraSource(context)
    val streamer = CameraSingleStreamer(
        context, source, hasAudio, endpointInternalFactory, defaultRotation
    )
    streamer.setVideoSource(source)
    return streamer
}

/**
 * A [SingleStreamer] with specific device camera methods.
 *
 * The [CameraSingleStreamer.videoSource] is a [CameraSource] and can't be changed.
 *
 * @param context the application context
 * @param cameraSource the camera source implementation.
 * @param hasAudio [Boolean.true] to capture audio
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class CameraSingleStreamer internal constructor(
    context: Context,
    private val cameraSource: CameraSource,
    hasAudio: Boolean = true,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : SingleStreamer(
    context = context,
    hasAudio = hasAudio,
    hasVideo = true,
    endpointInternalFactory = endpointInternalFactory,
    defaultRotation = defaultRotation
), ICameraCoroutineStreamer {
    /**
     * Gets the camera source.
     * It allows to configure camera settings and to set the camera id.
     */
    override val videoSource: ICameraSource by lazy { cameraSource }

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
        @RequiresPermission(Manifest.permission.CAMERA) set(value) {
            runBlocking {
                setCameraId(value)
            }
        }

    /**
     * Sets a camera id with a suspend function.
     *
     * @param cameraId The camera id to use
     */
    override suspend fun setCameraId(cameraId: String) = cameraSource.setCameraId(cameraId)

    /**
     * Sets a preview surface.
     */
    override suspend fun setPreview(surface: Surface) =
        cameraSource.setPreview(surface)

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
    override suspend fun startPreview() =
        cameraSource.startPreview()

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
    override suspend fun startPreview(previewSurface: Surface) =
        cameraSource.startPreview(previewSurface)

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    override suspend fun stopPreview() =
        cameraSource.stopPreview()

    /**
     * Same as [SingleStreamer.release] but it also calls [stopPreview].
     */
    override suspend fun release() {
        stopPreview()
        super.release()
    }

    override suspend fun setVideoSource(videoSource: IVideoSourceInternal) {
        require(videoSource is CameraSource) { "videoSource must be a CameraSource" }
        super.setVideoSource(videoSource)
    }
}