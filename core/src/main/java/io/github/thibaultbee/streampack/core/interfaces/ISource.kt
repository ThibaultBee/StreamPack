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
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.pipelines.inputs.IAudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.IVideoInput

/**
 * An audio single Streamer
 */
interface IWithAudioSource {
    /**
     * The audio input to access to advanced settings.
     */
    val audioInput: IAudioInput?

    /**
     * Sets a new audio source.
     *
     * @param audioSourceFactory The new audio source factory.
     */
    suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) {
        val audio = requireNotNull(audioInput) { "Audio input is not available" }
        audio.setSource(audioSourceFactory)
    }
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
     * The audio input to access to advanced settings.
     */
    val videoInput: IVideoInput?

    /**
     * Sets the video source.
     *
     * The previous video source will be released unless its preview is still running.
     */
    suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory) {
        val video = requireNotNull(videoInput) { "Video input is not available" }
        video.setSource(videoSourceFactory)
    }
}

/**
 * Sets the camera id.
 *
 * Same as [IWithVideoSource.videoInput.setSource] with a [CameraSourceFactory].
 *
 * @param cameraId the camera id
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IWithVideoSource.setCameraId(cameraId: String) {
    val video = requireNotNull(videoInput) { "Video input is not available" }
    video.setSource(CameraSourceFactory(cameraId))
}


/**
 * Whether the video source has a preview.
 */
val IWithVideoSource.isPreviewable: Boolean
    get() = videoInput?.sourceFlow?.value is IPreviewableSource

/**
 * Sets the preview surface.
 *
 * @param surface The [Surface] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(surface: Surface) {
    (videoInput?.sourceFlow?.value as? IPreviewableSource)?.setPreview(surface)
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
    (videoInput?.sourceFlow?.value as? IPreviewableSource)?.startPreview()
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
    (videoInput?.sourceFlow?.value as? IPreviewableSource)?.startPreview(surface)
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
    (videoInput?.sourceFlow?.value as? IPreviewableSource)?.stopPreview()
}