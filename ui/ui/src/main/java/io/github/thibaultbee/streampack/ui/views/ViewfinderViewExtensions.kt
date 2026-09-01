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

import android.graphics.Rect
import android.util.Size
import android.view.SurfaceHolder
import android.view.View
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest.Companion.MIRROR_MODE_HORIZONTAL
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import io.github.thibaultbee.streampack.ui.utils.PreviewConfigurationMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "ViewfinderViewExt"

/**
 * Binds a [IWithVideoSource] to a [ViewfinderView].
 *
 * It automatically handles:
 * 1. Lifecycle-aware observing of the streamer's video source and configuration.
 * 2. Surface configuration using [ViewfinderView.requestSurface].
 * 3. Gesture routing (pinch-to-zoom and tap-to-focus) to the underlying [IPreviewableSource].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe
 * @param streamer The streamer to preview
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 */
@ExperimentalStreamPackApi
fun ViewfinderView.bindToStreamerSource(
    lifecycleOwner: LifecycleOwner,
    streamer: IWithVideoSource,
    isPinchToZoomEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true
) {
    unbind() // Clear previous bindings if any

    // 1. Forward View Gestures to Camera Settings
    this.onZoomRatioChanged = { scale ->
        val source = streamer.videoInput.sourceFlow.value
        if (source is IPreviewableSource) {
            lifecycleOwner.lifecycleScope.launch {
                source.setZoomOnPinch(scale)
            }
        }
    }

    this.onTapToFocus = { point, _ ->
        val source = streamer.videoInput.sourceFlow.value
        if (source is IPreviewableSource) {
            lifecycleOwner.lifecycleScope.launch {
                source.setTapToFocus(
                    point,
                    Rect(
                        this@bindToStreamerSource.x.toInt(),
                        this@bindToStreamerSource.y.toInt(),
                        this@bindToStreamerSource.width,
                        this@bindToStreamerSource.height
                    ),
                    OrientationUtils.getSurfaceRotationDegrees(display.rotation)
                )
            }
        }
    }

    // 2. React to Source & Config Changes and Feed Surface
    val job = lifecycleOwner.lifecycleScope.launch {
        var previewJob: Job? = null
        try {
            streamer.videoInput.sourceFlow.collect { source ->
                previewJob?.cancel()

                val previewableSource = source as? IPreviewableSource
                if (previewableSource == null) {
                    this@bindToStreamerSource.isPinchToZoomEnabled = false
                    this@bindToStreamerSource.isTapToFocusEnabled = false
                    return@collect
                }

                this@bindToStreamerSource.isPinchToZoomEnabled =
                    previewableSource.isPinchToZoomSupported && isPinchToZoomEnabled
                this@bindToStreamerSource.isTapToFocusEnabled =
                    previewableSource.isTapToFocusSupported && isTapToFocusEnabled

                previewJob = launch {
                    try {
                        combine(
                            streamer.videoInput.sourceConfigFlow.map { it?.resolution }
                                .distinctUntilChanged(),
                            layoutSizeFlow()
                        ) { resolution, viewSize ->
                            previewableSource.getPreviewSize(
                                resolution ?: viewSize,
                                SurfaceHolder::class.java
                            )
                        }
                        .distinctUntilChanged()
                        .collect { previewSize ->
                            try {
                                previewableSource.previewMutex.withLock {
                                    previewableSource.stopPreview()
                                    previewableSource.resetPreview()
                                }

                                val surface = requestSurface(previewSize, previewableSource.previewConfiguration)

                                previewableSource.previewMutex.withLock {
                                    previewableSource.startPreview(surface)
                                }
                            } catch (e: Exception) {
                                Logger.e(TAG, "Failed to bind video source", e)
                            }
                        }
                    } finally {
                        withContext(NonCancellable) {
                            previewableSource.previewMutex.withLock {
                                previewableSource.stopPreview()
                                previewableSource.resetPreview()
                            }
                        }
                    }
                }
            }
        } finally {
            previewJob?.cancel()
            this@bindToStreamerSource.isPinchToZoomEnabled = false
            this@bindToStreamerSource.isTapToFocusEnabled = false
            this@bindToStreamerSource.onZoomRatioChanged = null
            this@bindToStreamerSource.onTapToFocus = null
        }
    }
    setTag(R.id.viewfinder_bind_job, job)
}

/**
 * Unbinds the [ViewfinderView] from its currently bound [IWithVideoSource].
 *
 * This stops observing the video source, cancels the surface request job,
 * and clears the tap and zoom gesture callbacks.
 */
@ExperimentalStreamPackApi
fun ViewfinderView.unbind() {
    (getTag(R.id.viewfinder_bind_job) as? Job)?.cancel()
    setTag(R.id.viewfinder_bind_job, null)
}

private fun View.layoutSizeFlow() = callbackFlow {
    val listener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val width = right - left
            val height = bottom - top
            val oldWidth = oldRight - oldLeft
            val oldHeight = oldBottom - oldTop
            if (width != oldWidth || height != oldHeight) {
                trySend(Size(width, height))
            }
        }
    addOnLayoutChangeListener(listener)
    if (width > 0 && height > 0) {
        trySend(Size(width, height))
    }
    awaitClose {
        removeOnLayoutChangeListener(listener)
    }
}.distinctUntilChanged()
