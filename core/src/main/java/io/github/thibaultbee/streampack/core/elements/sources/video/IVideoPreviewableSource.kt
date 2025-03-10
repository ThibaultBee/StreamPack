/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.video

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.setPreview
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import kotlinx.coroutines.flow.StateFlow

interface IVideoPreviewableSource {
    /**
     * Flow of the last previewing state.
     */
    val isPreviewingFlow: StateFlow<Boolean>

    /**
     * Sets preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    suspend fun setPreview(surface: Surface)

    /**
     * Starts video preview.
     */
    suspend fun startPreview()

    /**
     * Sets preview surface and start video preview.
     *
     * @param previewSurface The [Surface] used for camera preview
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
suspend fun IVideoPreviewableSource.setPreview(surfaceView: SurfaceView) =
    setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 */
suspend fun IVideoPreviewableSource.setPreview(surfaceHolder: SurfaceHolder) =
    setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for camera preview
 */
suspend fun IVideoPreviewableSource.setPreview(textureView: TextureView) =
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
suspend fun IVideoPreviewableSource.startPreview(surfaceView: SurfaceView) =
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
suspend fun IVideoPreviewableSource.startPreview(surfaceHolder: SurfaceHolder) =
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
suspend fun IVideoPreviewableSource.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))