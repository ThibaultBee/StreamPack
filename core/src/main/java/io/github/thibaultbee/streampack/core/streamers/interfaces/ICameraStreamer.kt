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
package io.github.thibaultbee.streampack.core.streamers.interfaces

import android.Manifest
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer

interface ICameraStreamer {
    /**
     * The camera source settings.
     */
    val videoSource: ICameraSource

    /**
     * Gets/Sets current camera id.
     * It is a shortcut for [ICameraSource.cameraId].
     */
    var cameraId: String
}

interface ICameraCoroutineStreamer : ICameraStreamer {
    /**
     * Sets a camera id.
     *
     * @param cameraId The camera id to use
     */
    suspend fun setCameraId(cameraId: String)

    /**
     * Sets a preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    suspend fun setPreview(surface: Surface)

    /**
     * Starts video preview.
     */
    suspend fun startPreview()

    /**
     * Starts video preview on [previewSurface].
     */
    suspend fun startPreview(previewSurface: Surface)

    /**
     * Stops video preview.
     */
    suspend fun stopPreview()
}

/**
 * Sets a preview surface.
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 */
suspend fun ICameraCoroutineStreamer.setPreview(surfaceView: SurfaceView) =
    setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 */
suspend fun ICameraCoroutineStreamer.setPreview(surfaceHolder: SurfaceHolder) =
    setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for camera preview
 */
suspend fun ICameraCoroutineStreamer.setPreview(textureView: TextureView) =
    setPreview(Surface(textureView.surfaceTexture))

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 *
 * @see [ICameraCoroutineStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun ICameraCoroutineStreamer.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 *
 * @see [ICameraCoroutineStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun ICameraCoroutineStreamer.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param textureView The [TextureView] used for camera preview
 *
 * @see [ICameraCoroutineStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun ICameraCoroutineStreamer.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))


interface ICameraCallbackStreamer : ICameraStreamer {
    /**
     * Sets a preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    fun setPreview(surface: Surface)

    /**
     * Starts audio and video capture.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    fun startPreview()

    /**
     * Stops video preview.
     */
    fun stopPreview()

    /**
     * Starts video preview on [previewSurface].
     */
    fun startPreview(previewSurface: Surface)
}

/**
 * Starts audio and video capture.
 * If you can prefer to call [ISingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 *
 * @see [ICameraCallbackStreamer.stopPreview]
 */
fun ICameraCallbackStreamer.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [ISingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 *
 * @see [ICameraCallbackStreamer.stopPreview]
 */
fun ICameraCallbackStreamer.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [ISingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param textureView The [TextureView] used for camera preview
 *
 * @see [ICameraCallbackStreamer.stopPreview]
 */
fun ICameraCallbackStreamer.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))
