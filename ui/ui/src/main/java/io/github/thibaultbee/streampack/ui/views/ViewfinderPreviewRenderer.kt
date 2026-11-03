/*
 * Copyright 2026 Thibault B.
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

import android.content.Context
import android.graphics.Matrix
import android.util.Size
import android.view.Surface
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest.Companion.MIRROR_MODE_HORIZONTAL
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.utils.PreviewConfigurationMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A [PreviewRenderer] that displays the raw frames of a video source with an AndroidX
 * [CameraViewfinder].
 *
 * The [CameraViewfinder] handles the rotation, the mirroring and the scaling of the frames.
 */
@OptIn(ExperimentalStreamPackApi::class)
internal class ViewfinderPreviewRenderer(context: Context) : PreviewRenderer {
    private var surfaceRequest: ViewfinderSurfaceRequest? = null

    override val view = CameraViewfinder(context)

    override fun setScaleType(scaleType: ScaleType) {
        view.scaleType = scaleType
    }

    override fun setBackgroundColor(color: Int) {
        view.setBackgroundColor(color)
    }

    override fun getContentToViewMatrix(
        viewWidth: Int,
        viewHeight: Int,
        scaleType: ScaleType
    ): Matrix? {
        val request = surfaceRequest ?: return null
        val surfaceSize = request.resolution
        if (surfaceSize.width == 0 || surfaceSize.height == 0) {
            return null
        }
        val displayRotationDegrees = OrientationUtils.getSurfaceRotationDegrees(
            view.display?.rotation ?: Surface.ROTATION_0
        )
        val rotationDegrees = (request.sourceOrientation - displayRotationDegrees + 360) % 360
        return InteractivePreviewView.getContentToViewMatrix(
            viewWidth,
            viewHeight,
            surfaceSize.rotate(rotationDegrees),
            scaleType
        )
    }

    /**
     * Requests a [Surface] from the viewfinder with a specific size and configuration.
     *
     * @param size The requested surface size.
     * @param configuration The preview configuration.
     * @return The [Surface] provided by the viewfinder.
     */
    suspend fun requestSurface(
        size: Size,
        configuration: IPreviewableSource.PreviewConfiguration
    ): Surface {
        val builder = ViewfinderSurfaceRequest.Builder(size)
        val orientationDegrees = configuration.orientationDegrees
            ?: OrientationUtils.getSurfaceRotationDegrees(
                view.display?.rotation ?: Surface.ROTATION_0
            )
        builder.setSourceOrientation(orientationDegrees)

        configuration.implementationMode?.let {
            builder.setImplementationMode(
                PreviewConfigurationMapper.toViewfinderImplementationMode(it)
            )
        }

        if (configuration.isSourceMirroredHorizontally) {
            builder.setOutputMirrorMode(MIRROR_MODE_HORIZONTAL)
        }

        return withContext(Dispatchers.Main) {
            surfaceRequest?.markSurfaceSafeToRelease()
            val request = builder.build().apply {
                surfaceRequest = this
            }
            view.requestSurface(request)
        }
    }

    /**
     * Releases the current [Surface] requested via [requestSurface].
     */
    fun releaseSurface() {
        surfaceRequest?.markSurfaceSafeToRelease()
        surfaceRequest = null
    }
}
