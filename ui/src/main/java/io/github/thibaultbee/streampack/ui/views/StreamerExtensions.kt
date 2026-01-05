package io.github.thibaultbee.streampack.ui.views

import android.util.Size
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession
import androidx.camera.viewfinder.view.ViewfinderView
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource

/**
 * Sets preview on a [ViewfinderView]
 *
 * @param viewfinder The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(
    viewfinder: ViewfinderView,
    previewSize: Size
): ViewfinderSurfaceSession {
    val videoSource = videoInput?.sourceFlow?.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    return videoSource.setPreview(viewfinder, previewSize)
}

/**
 * Starts preview on a [ViewfinderView]
 *
 * @param viewfinder The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.startPreview(
    viewfinder: ViewfinderView,
    previewSize: Size
): ViewfinderSurfaceSession {
    val videoSource = videoInput?.sourceFlow?.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    return videoSource.startPreview(viewfinder, previewSize)
}
