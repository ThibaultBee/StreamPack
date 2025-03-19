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
package io.github.thibaultbee.streampack.core.streamers.interfaces

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
import io.github.thibaultbee.streampack.core.streamers.single.startStream
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

interface IStreamer

/**
 * A single Streamer based on coroutines.
 */
interface ICoroutineStreamer : IStreamer {
    /**
     * Returns the last throwable that occurred.
     */
    val throwableFlow: StateFlow<Throwable?>

    /**
     * Returns true if the streamer is opened.
     * For example, if the streamer is connected to a server if the endpoint is SRT or RTMP.
     */
    val isOpenFlow: StateFlow<Boolean>

    /**
     * Closes the streamer.
     */
    suspend fun close()

    /**
     * Returns true if stream is running.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * Starts audio/video stream.
     *
     * @see [stopStream]
     */
    suspend fun startStream()

    /**
     * Stops audio/video stream.
     *
     * @see [startStream]
     */
    suspend fun stopStream()

    /**
     * Clean and reset the streamer.
     */
    suspend fun release()
}

/**
 * Clean and reset the streamer synchronously.
 *
 * @see [ICoroutineStreamer.release]
 */
fun ICoroutineStreamer.releaseBlocking() = runBlocking {
    release()
}


interface ICoroutineAudioStreamer<T> {
    /**
     * Configures only audio settings.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setAudioConfig(audioConfig: T)
}

interface ICoroutineVideoStreamer<T> {
    /**
     * Configures only video settings.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    suspend fun setVideoConfig(videoConfig: T)
}

/**
 * An audio single Streamer
 */
interface IAudioStreamer {
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

/**
 * A video single streamer.
 */
interface IVideoStreamer {
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
 * Same as [setVideoSource] with a [CameraSourceFactory].
 *
 * @param cameraId the camera id
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IVideoStreamer.setCameraId(cameraId: String) =
    setVideoSource(CameraSourceFactory(cameraId))

/**
 * Whether the video source has a preview.
 */
val IVideoStreamer.isPreviewable: Boolean
    get() = videoSourceFlow.value is IPreviewableSource

/**
 * Sets the preview surface.
 *
 * @param surface The [Surface] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.setPreview(surface: Surface) {
    (videoSourceFlow.value as? IPreviewableSource)?.setPreview(surface)
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets a preview surface.
 *
 * @param surfaceView The [SurfaceView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.setPreview(surfaceView: SurfaceView) =
    setPreview(surfaceView.holder.surface)

/**
 * Sets a preview surface holder.
 *
 * @param surfaceHolder The [SurfaceHolder] used the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.setPreview(surfaceHolder: SurfaceHolder) =
    setPreview(surfaceHolder.surface)

/**
 * Sets a preview surface.
 *
 * @param textureView The [TextureView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.setPreview(textureView: TextureView) =
    setPreview(Surface(textureView.surfaceTexture))

/**
 * Starts video preview.
 *
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.startPreview() {
    (videoSourceFlow.value as? IPreviewableSource)?.startPreview()
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets preview surface and start video preview.
 *
 * @param surface The [Surface] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IVideoStreamer.stopPreview]
 */
suspend fun IVideoStreamer.startPreview(surface: Surface) {
    (videoSourceFlow.value as? IPreviewableSource)?.startPreview(surface)
        ?: throw IllegalStateException("Video source is not previewable")
}

/**
 * Sets preview surface and start video preview.
 *
 * @param surfaceView The [SurfaceView] used for the source preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IVideoStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IVideoStreamer.startPreview(surfaceView: SurfaceView) =
    startPreview(surfaceView.holder.surface)

/**
 * Sets preview surface and start video preview.
 *
 * @param surfaceHolder The [SurfaceHolder] used for camera preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IVideoStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IVideoStreamer.startPreview(surfaceHolder: SurfaceHolder) =
    startPreview(surfaceHolder.surface)

/**
 * Sets preview surface and start video preview.
 *
 * @param textureView The [TextureView] used for camera preview
 * @throws [IllegalStateException] if the video source is not previewable
 * @see [IVideoStreamer.stopPreview]
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun IVideoStreamer.startPreview(textureView: TextureView) =
    startPreview(Surface(textureView.surfaceTexture))

/**
 * Stops video preview.
 */
suspend fun IVideoStreamer.stopPreview() {
    (videoSourceFlow.value as? IPreviewableSource)?.stopPreview()
}