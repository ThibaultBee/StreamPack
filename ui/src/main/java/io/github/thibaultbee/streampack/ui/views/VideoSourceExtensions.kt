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
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession
import androidx.camera.viewfinder.view.ViewfinderView
import androidx.camera.viewfinder.view.requestSurfaceSession
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource

/**
 * Sets preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 */
suspend fun IPreviewableSource.setPreview(
    viewfinderView: ViewfinderView,
    previewSize: Size
) = setPreview(viewfinderView, ViewfinderSurfaceRequest(previewSize.width, previewSize.height))

/**
 * Sets preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param surfaceRequest The [ViewfinderSurfaceRequest] used to set the preview.
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 */
suspend fun IPreviewableSource.setPreview(
    viewfinderView: ViewfinderView,
    surfaceRequest: ViewfinderSurfaceRequest
): ViewfinderSurfaceSession {
    val surfaceSession = viewfinderView.requestSurfaceSession(
        surfaceRequest
    )
    setPreview(surfaceSession.surface)
    return surfaceSession
}

/**
 * Starts preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 */
suspend fun IPreviewableSource.startPreview(
    viewfinderView: ViewfinderView,
    previewSize: Size
): ViewfinderSurfaceSession {
    val surfaceSession = setPreview(viewfinderView, previewSize)
    startPreview()
    return surfaceSession
}

/**
 * Starts preview on a [ViewfinderView]
 *
 * @param viewfinderView The [ViewfinderView] to set as preview
 * @param surfaceRequest The [ViewfinderSurfaceRequest] used to set the preview.
 * @return The [ViewfinderSurfaceSession] used to set the preview. Use it to call [ViewfinderSurfaceSession.close].
 */
suspend fun IPreviewableSource.startPreview(
    viewfinderView: ViewfinderView,
    surfaceRequest: ViewfinderSurfaceRequest
): ViewfinderSurfaceSession {
    val surfaceSession = setPreview(viewfinderView, surfaceRequest)
    startPreview()
    return surfaceSession
}
