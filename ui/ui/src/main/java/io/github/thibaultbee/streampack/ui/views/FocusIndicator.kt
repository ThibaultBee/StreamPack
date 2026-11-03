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

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.MainThread
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi

/**
 * Interface for providing feedback during tap-to-focus operations.
 *
 * Implementations may be a [android.view.View] (such as [DefaultFocusIndicator]) or any custom
 * controller or state holder.
 *
 * All callbacks are invoked on the main thread.
 */
@ExperimentalStreamPackApi
interface FocusIndicator {
    /**
     * Called when tap-to-focus starts at the specified [point] in [InteractivePreviewView] coordinates.
     *
     * May be called again while a previous focus operation is still in progress if the user
     * taps a new location.
     *
     * @param point The touch coordinates on the [InteractivePreviewView] in pixels.
     */
    @MainThread
    fun onFocusStart(point: PointF)

    /**
     * Called when tap-to-focus starts at the specified [point] in [InteractivePreviewView] coordinates,
     * providing the visible [bounds] of the preview content.
     *
     * The default implementation forwards to [onFocusStart] with [point].
     *
     * @param point The touch coordinates on the [InteractivePreviewView] in pixels.
     * @param bounds The bounds of the displayed video content in [InteractivePreviewView] coordinates,
     * or `null` if unknown.
     */
    @MainThread
    fun onFocusStart(point: PointF, bounds: RectF?) {
        onFocusStart(point)
    }

    /**
     * Called when the active focus operation succeeds.
     */
    @MainThread
    fun onFocusSuccess()

    /**
     * Called when the active focus operation fails.
     */
    @MainThread
    fun onFocusFailed()

    /**
     * Called when the active focus operation is cancelled or reset.
     */
    @MainThread
    fun onFocusCancel()
}
