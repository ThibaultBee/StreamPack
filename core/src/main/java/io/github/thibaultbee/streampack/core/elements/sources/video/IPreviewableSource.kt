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

import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Interface for video sources that can be previewed.
 *
 * The methods of this interface should be called in the [previewMutex].
 */
@SubclassOptInRequired(InternalStreamPackApi::class)
interface IPreviewableSource {
    enum class ImplementationMode {
        /**
         * Based on TextureView
         */
        COMPATIBLE,

        /**
         * Based on SurfaceView
         */
        PERFORMANCE
    }

    data class PreviewConfiguration(
        val orientationDegrees: Int? = null,
        val isSourceMirroredHorizontally: Boolean = false,
        val implementationMode: ImplementationMode? = null,
    )

    /**
     * Configuration for the preview surface, such as orientation, implementation mode, and mirror mode.
     */
    val previewConfiguration: PreviewConfiguration
        get() = PreviewConfiguration()

    /**
     * Whether the video source supports pinch to zoom.
     */
    val isPinchToZoomSupported: Boolean
        get() = false

    /**
     * Sets the zoom on pinch scale gesture.
     *
     * In case [isPinchToZoomSupported] is false, this method should do nothing.
     *
     * @param scale the scale factor. Typically, values > 1.0 should zoom in and values between 0.0 and 1.0 should zoom out (dezoom).
     */
    suspend fun setZoomOnPinch(scale: Float) {}

    /**
     * Whether the video source supports tap to focus.
     */
    val isTapToFocusSupported: Boolean
        get() = false

    /**
     * Sets tap to focus.
     *
     * In case [isTapToFocusSupported] is false, this method should do nothing.
     *
     * @param point the point to focus on in [fovRect] coordinate system
     * @param fovRect the field of view rectangle
     * @param fovRotationDegree the orientation of the field of view
     */
    suspend fun setTapToFocus(point: PointF, fovRect: Rect, fovRotationDegree: Int) {}

    /**
     * Mutex for the preview.
     * Use it when you have to synchronise access to the preview.
     * It is used by the [IPreviewableSource] user.
     */
    val previewMutex: Mutex

    /**
     * Flow of the last previewing state.
     */
    val isPreviewingFlow: StateFlow<Boolean>

    /**
     * Whether the video source has a preview.
     *
     * @return `true` if the video source has a preview, `false`
     */
    suspend fun hasPreview(): Boolean

    /**
     * Sets preview surface.
     *
     * @param surface The [Surface] used for camera preview
     */
    suspend fun setPreview(surface: Surface)

    /**
     * Starts video preview.
     *
     * In some particular case, [startPreview] can be called several time without any call to [stopPreview].
     */
    suspend fun startPreview()

    /**
     * Sets preview surface and start video preview.
     *
     * @param surface The [Surface] used for camera preview
     */
    suspend fun startPreview(surface: Surface) {
        setPreview(surface)
        try {
            startPreview()
        } catch (t: Throwable) {
            withContext(NonCancellable) {
                resetPreview()
            }
            throw t
        }
    }

    /**
     * Stops video preview.
     */
    suspend fun stopPreview()

    /**
     * Resets preview.
     *
     * When the preview is reset, the [IPreviewableSource] should stop sending frames to the surface set
     * by [setPreview]. The implementation must forget the previous surface.
     */
    suspend fun resetPreview()

    /**
     * Requests to release the video source.
     *
     * When set a new video source, if the preview is in progress, the video source is not released.
     * In that case, [requestRelease] is called and the video source is released when the preview
     * and the output are reset.
     *
     * The implementation must keep the request release state until all surfaces are reset.
     *
     * To be called by the user.
     */
    suspend fun requestRelease()

    /**
     * Gets the preview size from the target size.
     *
     * @param targetSize the target size
     * @param targetClass the output target class. Only used for [CameraSource].
     * @return the preview size
     */
    fun <T> getPreviewSize(targetSize: Size, targetClass: Class<T>): Size
}

/**
 * Sets a preview surface.
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 */
suspend fun IPreviewableSource.setPreview(surfaceView: SurfaceView) =
    setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 */
suspend fun IPreviewableSource.setPreview(surfaceHolder: SurfaceHolder) =
    setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for camera preview
 */
suspend fun IPreviewableSource.setPreview(textureView: TextureView) =
    setPreview(Surface(textureView.surfaceTexture))

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceView The [SurfaceView] used for camera preview
 *
 * @see [IPreviewableSource.stopPreview]
 */
suspend fun IPreviewableSource.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 *
 * @see [IPreviewableSource.stopPreview]
 */
suspend fun IPreviewableSource.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Starts audio and video capture.
 * If you can prefer to call [SingleStreamer.setAudioConfig] before starting preview.
 * It is a shortcut for [setPreview] and [startPreview].
 *
 * @param textureView The [TextureView] used for camera preview
 *
 * @see [IPreviewableSource.stopPreview]
 */
suspend fun IPreviewableSource.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))