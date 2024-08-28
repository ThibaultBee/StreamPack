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
import io.github.thibaultbee.streampack.core.internal.sources.video.camera.IPublicCameraSource
import io.github.thibaultbee.streampack.core.streamers.DefaultStreamer

interface ICameraStreamer {
    /**
     * The camera source settings.
     */
    val videoSource: IPublicCameraSource

    /**
     * Gets/Sets current camera id.
     * It is a shortcut for [videoSource.cameraId].
     */
    var camera: String

    /**
     * Sets a preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    fun setPreview(surface: Surface)

    /**
     * Sets a preview surface.
     *
     * @param surfaceView The [SurfaceView] used for camera preview
     */
    fun setPreview(surfaceView: SurfaceView) = setPreview(surfaceView.holder.surface)

    /**
     * Sets a preview surface holder.
     *
     * @param surfaceHolder The [SurfaceHolder] used for camera preview
     */
    fun setPreview(surfaceHolder: SurfaceHolder) = setPreview(surfaceHolder.surface)

    /**
     * Sets a preview surface.
     *
     * @param textureView The [TextureView] used for camera preview
     */
    fun setPreview(textureView: TextureView) = setPreview(Surface(textureView.surfaceTexture))

    /**
     * Starts audio and video capture.
     */
    suspend fun startPreview()

    /**
     * Starts audio and video capture.
     * [DefaultStreamer.configure] must have been called at least once.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param previewSurface The [Surface] used for camera preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    suspend fun startPreview(previewSurface: Surface) {
        setPreview(previewSurface)
        startPreview()
    }

    /**
     * Starts audio and video capture.
     * [DefaultStreamer.configure] must have been called at least once.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param surfaceView The [SurfaceView] used for camera preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    suspend fun startPreview(surfaceView: SurfaceView) =
        startPreview(surfaceView.holder.surface)

    /**
     * Starts audio and video capture.
     * [DefaultStreamer.configure] must have been called at least once.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param surfaceHolder The [SurfaceHolder] used for camera preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    suspend fun startPreview(surfaceHolder: SurfaceHolder) =
        startPreview(surfaceHolder.surface)

    /**
     * Starts audio and video capture.
     * [DefaultStreamer.configure] must have been called at least once.
     * It is a shortcut for [setPreview] and [startPreview].
     *
     * @param textureView The [TextureView] used for camera preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    suspend fun startPreview(textureView: TextureView) =
        startPreview(Surface(textureView.surfaceTexture))

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    fun stopPreview()
}