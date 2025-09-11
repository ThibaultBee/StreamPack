/*
 * Copyright 2025 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.ui.views

import android.util.Size
import androidx.camera.viewfinder.view.ViewfinderView
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource

/**
 * Set preview on a [ViewfinderView]
 *
 * @param viewfinder The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.setPreview(
    viewfinder: ViewfinderView,
    previewSize: Size
) {
    val videoSource = videoInput?.sourceFlow?.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    videoSource.setPreview(viewfinder, previewSize)
}

/**
 * Start preview on a [ViewfinderView]
 *
 * @param viewfinder The [ViewfinderView] to set as preview
 * @param previewSize The size of the preview
 * @throws [IllegalStateException] if the video source is not previewable
 */
suspend fun IWithVideoSource.startPreview(
    viewfinder: ViewfinderView,
    previewSize: Size
) {
    val videoSource = videoInput?.sourceFlow?.value as? IPreviewableSource
        ?: throw IllegalStateException("Video source is not previewable")
    videoSource.startPreview(viewfinder, previewSize)
}
