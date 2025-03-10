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
package io.github.thibaultbee.streampack.ui.views

import android.util.Size
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.populateFromCharacteristics
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.getCameraCharacteristics

/**
 * Start preview on a [CameraViewfinder]
 *
 * @param viewfinder The [CameraViewfinder] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceRequest] used to set the preview. Use it to call [ViewfinderSurfaceRequest.markSurfaceSafeToRelease] after [stopPreview].
 */
suspend fun IVideoPreviewableSource.startPreview(
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
suspend fun IVideoPreviewableSource.setPreview(
    viewfinder: CameraViewfinder,
    previewSize: Size
): ViewfinderSurfaceRequest {
    val builder = ViewfinderSurfaceRequest.Builder(previewSize)
    val request = if (this is ICameraSource) {
        val cameraCharacteristics = viewfinder.context.getCameraCharacteristics(cameraId)
        builder.populateFromCharacteristics(cameraCharacteristics).build()
    } else {
        builder.build()
    }
    setPreview(viewfinder.requestSurface(request))
    return request
}
