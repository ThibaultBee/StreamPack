package io.github.thibaultbee.streampack.ui.views

import android.util.Size
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer

/**
 * Set preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease].
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.setPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val videoSource = videoSourceFlow.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    return videoSource.setPreview(viewfinder, previewSize)
}

/**
 * Start preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease] after [IVideoStreamer.stopPreview].
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IVideoStreamer.startPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val videoSource = videoSourceFlow.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    return videoSource.startPreview(viewfinder, previewSize)
}
