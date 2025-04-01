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
package io.github.thibaultbee.streampack.core.interfaces

import android.Manifest
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.processing.audio.IAudioFrameProcessor
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import kotlinx.coroutines.flow.StateFlow

/**
 * An audio single Streamer
 */
interface IWithAudioSource {
    /**
     * An audio source flow to access to advanced settings.
     */
    val audioSourceFlow: StateFlow<IAudioSource?>

    /**
     * Advanced settings for the audio processor.
     */
    val audioProcessor: IAudioFrameProcessor?

    /**
     * Sets the audio source.
     *
     * @param audioSourceFactory The audio source factory to set
     */
    suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory)
}

interface IWithVideoRotation {
    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    suspend fun setTargetRotation(@RotationValue rotation: Int)
}

/**
 * A video single streamer.
 */
interface IWithVideoSource {
    /**
     * A video source flow to access to advanced settings.
     */
    val videoSourceFlow: StateFlow<IVideoSource?>

    /**
     * Sets the video source.
     *
     * @param videoSourceFactory The video source factory to set
     */
    suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory)
}

/**
 * Sets the camera id.
 *
 * Same as [IWithVideoSource.setVideoSource] with a [CameraSourceFactory].
 *
 * @param cameraId the camera id
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.setCameraId(cameraId: String) =
    setVideoSource(CameraSourceFactory(cameraId))

/**
 * Whether the video source has a preview.
 */
val IWithVideoSource.isPreviewable: Boolean
    get() = videoSourceFlow.value is IPreviewableSource

/**
 * Sets the preview surface.
 *
 * @param surface The [Surface] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(surface: Surface) {
    (videoSourceFlow.value as? IPreviewableSource)?.setPreview(surface)
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets a preview surface.
 *
 * @param surfaceView The [SurfaceView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(surfaceView: SurfaceView) =
    setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(surfaceHolder: SurfaceHolder) =
    setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(textureView: TextureView) =
    setPreview(Surface(textureView.surfaceTexture))

/**
 * Starts video preview.
 *
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.startPreview() {
    (videoSourceFlow.value as? IPreviewableSource)?.startPreview()
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets preview surface and start video preview.
 *
 * @param surface The [Surface] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IWithVideoSource.stopPreview]
 */
suspend fun IWithVideoSource.startPreview(surface: Surface) {
    (videoSourceFlow.value as? IPreviewableSource)?.startPreview(surface)
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets preview surface and start video preview.
 *
 * @param surfaceView The [SurfaceView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IWithVideoSource.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Sets preview surface and start video preview.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IWithVideoSource.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Sets preview surface and start video preview.
 *
 * @param textureView The [TextureView] used for camera preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IWithVideoSource.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))

/**
 * Stops video preview.
 */
suspend fun IWithVideoSource.stopPreview() {
    (videoSourceFlow.value as? IPreviewableSource)?.stopPreview()
}