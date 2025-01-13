package io.github.thibaultbee.streampack.ui.views

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.impl.utils.futures.FutureCallback
import androidx.camera.viewfinder.core.impl.utils.futures.Futures
import androidx.camera.viewfinder.core.populateFromCharacteristics
import androidx.core.content.ContextCompat
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.getCameraCharacteristics

/**
 * Start preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease] after [stopPreview].
 */
suspend fun ICameraCoroutineStreamer.startPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val request = setPreview(viewfinder, previewSize)
    startPreview()
    return request
}

/**
 * Set preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease].
 */
suspend fun ICameraCoroutineStreamer.setPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val builder = ViewfinderSurfaceRequest.Builder(previewSize)
    val cameraCharacteristics = viewfinder.context.getCameraCharacteristics(cameraId)
    val request = builder.populateFromCharacteristics(cameraCharacteristics).build()
    setPreview(viewfinder.requestSurface(request))
    return request
}

/**
 * Start preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease] after [stopPreview].
 */
fun ICameraCallbackStreamer.startPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val cameraCharacteristics = viewfinder.context.getCameraCharacteristics(cameraId)
    return executeSurfaceRequest(
        viewfinder.context,
        viewfinder,
        previewSize,
        cameraCharacteristics
    ) {
        startPreview(it)
    }
}

/**
 * Set preview on a [CameraViewfinder].
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease].
 */
fun ICameraCallbackStreamer.setPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val cameraCharacteristics = viewfinder.context.getCameraCharacteristics(cameraId)
    return executeSurfaceRequest(
        viewfinder.context,
        viewfinder,
        previewSize,
        cameraCharacteristics
    ) { setPreview(it) }
}

private fun executeSurfaceRequest(
    context: Context,
    viewfinder: CameraViewfinder,
    previewSize: Size,
    cameraCharacteristics: CameraCharacteristics,
    block: (Surface) -> Unit
): ViewfinderSurfaceRequest {
    val builder = ViewfinderSurfaceRequest.Builder(previewSize)
    val request = builder.populateFromCharacteristics(cameraCharacteristics).build()
    val surfaceListenableFuture = viewfinder.requestSurfaceAsync(request)
    Futures.addCallback(surfaceListenableFuture, object : FutureCallback<Surface> {
        override fun onSuccess(result: Surface?) {
            result?.let { block(it) }
        }

        override fun onFailure(t: Throwable) { /* something went wrong */
        }
    }, ContextCompat.getMainExecutor(context))

    return request
}