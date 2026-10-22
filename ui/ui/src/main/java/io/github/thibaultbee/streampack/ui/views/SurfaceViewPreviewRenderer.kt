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
import android.graphics.Color
import android.graphics.Matrix
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.camera.viewfinder.core.ScaleType
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [PreviewRenderer] that provides a [SurfaceView] for frames that are already rendered
 * (rotated, scaled and positioned) by another component, such as the streamer pipeline.
 *
 * The surface always has the size of the view, so this renderer does not transform the frames.
 *
 * @param context The [Context]
 * @param contentSizeProvider Provides the size of the video rendered into the surface, as oriented
 *   on the display. It is only used to locate the video within the view, for gestures.
 */
@OptIn(ExperimentalStreamPackApi::class)
internal class SurfaceViewPreviewRenderer(
    context: Context,
    private val contentSizeProvider: () -> Size?
) : PreviewRenderer {
    /**
     * A [Surface] and its size in pixels.
     */
    data class SurfaceInfo(val surface: Surface, val size: Size)

    private val _surfaceFlow = MutableStateFlow<SurfaceInfo?>(null)

    /**
     * The current [SurfaceInfo], or `null` if there is no valid surface.
     */
    val surfaceFlow: StateFlow<SurfaceInfo?> = _surfaceFlow.asStateFlow()

    @VisibleForTesting
    internal fun setSurfaceInfo(surfaceInfo: SurfaceInfo?) {
        _surfaceFlow.value = surfaceInfo
    }

    private val _backgroundColorFlow = MutableStateFlow(Color.BLACK)

    /**
     * The background color of the preview.
     */
    val backgroundColorFlow: StateFlow<Int> = _backgroundColorFlow.asStateFlow()

    /**
     * The background color of the preview.
     */
    val backgroundColor: Int
        @ColorInt get() = _backgroundColorFlow.value

    override fun setBackgroundColor(@ColorInt color: Int) {
        _backgroundColorFlow.value = color
    }

    private val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) = Unit

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            _surfaceFlow.value = SurfaceInfo(holder.surface, Size(width, height))
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            _surfaceFlow.value = null
        }
    }

    override val view = SurfaceView(context)

    init {
        view.holder.addCallback(callback)
    }

    override fun getContentToViewMatrix(
        viewWidth: Int,
        viewHeight: Int,
        scaleType: ScaleType
    ): Matrix? {
        val contentSize = contentSizeProvider() ?: return null
        if (contentSize.width <= 0 || contentSize.height <= 0) {
            return null
        }
        return InteractivePreviewView.getContentToViewMatrix(
            viewWidth,
            viewHeight,
            contentSize,
            scaleType
        )
    }

    /**
     * Stops listening to the surface.
     */
    fun release() {
        view.holder.removeCallback(callback)
        _surfaceFlow.value = null
    }
}
