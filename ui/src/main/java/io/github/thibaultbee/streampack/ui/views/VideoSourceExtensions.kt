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
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.view.ViewfinderView
import androidx.camera.viewfinder.view.requestSurfaceSession
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource

/**
 * Start preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 */
suspend fun IPreviewableSource.startPreview(
    viewfinderView: ViewfinderView,
    previewSize: Size
) {
    setPreview(viewfinderView, previewSize)
    startPreview()
}

/**
 * Set preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 */
suspend fun IPreviewableSource.setPreview(
    viewfinderView: ViewfinderView,
    previewSize: Size
) {
    val request = ViewfinderSurfaceRequest(previewSize.width, previewSize.height)

    /*
    val request = if (this is ICameraSource) {
        val cameraCharacteristics = viewfinder.context.getCameraCharacteristics(cameraId)
        builder.populateFromCharacteristics(cameraCharacteristics).build()
    } else {
        builder.build()
    }*/
    val session = viewfinderView.requestSurfaceSession(request)
    viewfinderView.transformationInfo = infoProviderFlow.value.toTransformationInfo()
    setPreview(session.surface)
}
