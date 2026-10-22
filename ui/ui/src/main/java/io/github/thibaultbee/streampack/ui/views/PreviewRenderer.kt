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

import android.graphics.Matrix
import android.view.View
import androidx.annotation.ColorInt
import androidx.camera.viewfinder.core.ScaleType

/**
 * Renders frames inside an [InteractivePreviewView].
 *
 * A renderer owns the child [view] that displays the frames. The [InteractivePreviewView] handles
 * the gestures and only asks the renderer where the video is displayed.
 */
internal interface PreviewRenderer {
    /**
     * The [View] that displays the frames.
     */
    val view: View

    /**
     * Applies the [ScaleType] of the [InteractivePreviewView].
     *
     * Renderers that let another component scale the frames can ignore it.
     */
    fun setScaleType(scaleType: ScaleType) = Unit

    /**
     * Applies the background color of the [InteractivePreviewView].
     *
     * @param color The background color as an ARGB integer.
     */
    fun setBackgroundColor(@ColorInt color: Int) = Unit

    /**
     * Gets a [Matrix] that transforms normalized video coordinates [0..1] to the view coordinate
     * space.
     *
     * @param viewWidth The view width in pixels.
     * @param viewHeight The view height in pixels.
     * @param scaleType The current [ScaleType].
     * @return The [Matrix], or `null` if the displayed video is unknown.
     */
    fun getContentToViewMatrix(viewWidth: Int, viewHeight: Int, scaleType: ScaleType): Matrix?
}
