/*
 * Copyright (C) 2022 Thibault B.
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
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer

/**
 * Sets a preview surface.
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 */
fun ICameraStreamer.setPreview(surfaceView: SurfaceView) = setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 */
fun ICameraStreamer.setPreview(surfaceHolder: SurfaceHolder) = setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for camera preview
 */
fun ICameraStreamer.setPreview(textureView: TextureView) =
    setPreview(Surface(textureView.surfaceTexture))


/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param previewSurface The [Surface] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
suspend fun ICameraCoroutineStreamer.startPreview(previewSurface: Surface) {
    setPreview(previewSurface)
    startPreview()
}

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
suspend fun ICameraCoroutineStreamer.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
suspend fun ICameraCoroutineStreamer.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param textureView The [TextureView] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
suspend fun ICameraCoroutineStreamer.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param previewSurface The [Surface] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
fun ICameraCallbackStreamer.startPreview(previewSurface: Surface) {
    setPreview(previewSurface)
    startPreview()
}

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
fun ICameraCallbackStreamer.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
fun ICameraCallbackStreamer.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [DefaultStreamer.configure] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param textureView The [TextureView] used for camera preview
 *
 * @see [stopPreview]
 */
@RequiresPermission(allOf = [Manifest.permission.CAMERA])
fun ICameraCallbackStreamer.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))