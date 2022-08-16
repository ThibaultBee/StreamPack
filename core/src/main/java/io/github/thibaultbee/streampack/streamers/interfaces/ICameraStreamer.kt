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
package io.github.thibaultbee.streampack.streamers.interfaces

import android.Manifest
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IBaseCameraStreamerSettings

interface ICameraStreamer {
    /**
     * Get/Set current camera id.
     */
    var camera: String

    /**
     *  Access extended camera settings.
     */
    val settings: IBaseCameraStreamerSettings

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * @param previewSurface The [Surface] used for camera preview
     * @param cameraId The camera id where to start preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface, cameraId: String = camera)

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * @param surfaceView The [SurfaceView] used for camera preview
     * @param cameraId The camera id where to start preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    fun startPreview(surfaceView: SurfaceView, cameraId: String = camera) =
        startPreview(surfaceView.holder.surface, cameraId)

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * @param surfaceHolder The [SurfaceHolder] used for camera preview
     * @param cameraId The camera id where to start preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    fun startPreview(surfaceHolder: SurfaceHolder, cameraId: String = camera) =
        startPreview(surfaceHolder.surface, cameraId)

    /**
     * Starts audio and video capture.
     * [BaseStreamer.configure] must have been called at least once.
     *
     * @param textureView The [TextureView] used for camera preview
     * @param cameraId The camera id where to start preview
     *
     * @see [stopPreview]
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    fun startPreview(textureView: TextureView, cameraId: String = camera) =
        startPreview(Surface(textureView.surfaceTexture), cameraId)

    /**
     * Stops capture.
     * It also stops stream if the stream is running.
     *
     * @see [startPreview]
     */
    fun stopPreview()
}