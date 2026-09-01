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
package io.github.thibaultbee.streampack.ui.utils

import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewControllableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import io.github.thibaultbee.streampack.ui.views.ViewfinderView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "ViewfinderViewExt"

/**
 * Binds a [IWithVideoSource] to a [io.github.thibaultbee.streampack.ui.views.ViewfinderView].
 *
 * It automatically handles:
 * 1. Lifecycle-aware observing of the streamer's video source and configuration.
 * 2. Surface configuration using [requestSurface].
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
    val previousJob = getTag(R.id.viewfinder_bind_job) as? Job
    previousJob?.cancel()

    val job = lifecycleOwner.lifecycleScope.launch {
        previousJob?.join()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            streamer.videoInput.sourceFlow.collectLatest { source ->
                val previewableSource = source as? IPreviewableSource ?: return@collectLatest
                val controllableSource = source as? IPreviewControllableSource

                val pinchToZoomListener =
                    if (isPinchToZoomEnabled && controllableSource?.isZoomOnPinchSupported == true) {
                        createPinchToZoomListener(this, controllableSource).also {
                            addOnPinchToZoomListener(it)
                        }
                    } else {
                        null
                    }

                val tapToFocusListener =
                    if (isTapToFocusEnabled && controllableSource?.isTapToFocusSupported == true) {
                        createTapToFocusListener(this, controllableSource).also {
                            addOnTapToFocusListener(it)
                        }
                    } else {
                        null
                    }

                try {
                    feedPreview(streamer, previewableSource)
                } finally {
                    pinchToZoomListener?.let { removeOnPinchToZoomListener(it) }
                    tapToFocusListener?.let { removeOnTapToFocusListener(it) }
                    previewableSource.stopAndResetPreview(
                        shouldRelease = streamer.videoInput.sourceFlow.value != source
                    )
                    releaseSurface()
                }
            }
        }
    }
    setTag(R.id.viewfinder_bind_job, job)
}

/**
 * Unbinds the [ViewfinderView] from its currently bound [IWithVideoSource].
 *
 * This stops observing the video source, cancels the surface request job,
 * and removes the internal tap and zoom gesture listeners added by [bindToStreamerSource].
 */
@ExperimentalStreamPackApi
fun ViewfinderView.unbind() {
    (getTag(R.id.viewfinder_bind_job) as? Job)?.cancel()
    setTag(R.id.viewfinder_bind_job, null)
}

@OptIn(ExperimentalStreamPackApi::class)
private fun createPinchToZoomListener(
    scope: CoroutineScope,
    source: IPreviewControllableSource
) = ViewfinderView.OnPinchToZoomListener { scale, pinchCenter ->
    scope.launch {
        try {
            source.setZoomOnPinch(scale, pinchCenter)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Logger.w(TAG, "Failed to set zoom on pinch: ${t.message}")
        }
    }
}

@OptIn(ExperimentalStreamPackApi::class)
private fun ViewfinderView.createTapToFocusListener(
    scope: CoroutineScope,
    source: IPreviewControllableSource
) = ViewfinderView.OnTapToFocusListener { point, meteringPointFactory ->
    val displayRotation = display?.rotation ?: Surface.ROTATION_0
    val rotationDegrees = OrientationUtils.getSurfaceRotationDegrees(displayRotation)
    scope.launch {
        try {
            source.tapToFocus(
                point,
                meteringPointFactory,
                rotationDegrees,
                autoCancelDuration
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Logger.w(TAG, "Failed to focus at $point: ${t.message}")
        }
    }
}

@OptIn(ExperimentalStreamPackApi::class)
private suspend fun ViewfinderView.feedPreview(
    streamer: IWithVideoSource,
    previewableSource: IPreviewableSource
) {
    combine(
        streamer.videoInput.sourceConfigFlow.map { it?.resolution }.distinctUntilChanged(),
        layoutSizeFlow()
    ) { resolution, viewSize ->
        previewableSource.getPreviewSize(
            resolution ?: viewSize,
            SurfaceHolder::class.java
        )
    }
        .distinctUntilChanged()
        .collectLatest { previewSize ->
            startPreview(previewableSource, previewSize)
        }
}

@OptIn(ExperimentalStreamPackApi::class)
private suspend fun ViewfinderView.startPreview(
    previewableSource: IPreviewableSource,
    previewSize: Size
) {
    try {
        previewableSource.stopAndResetPreview()

        val surface = requestSurface(
            previewSize,
            previewableSource.previewConfiguration
        )

        if (surface.isValid) {
            previewableSource.previewMutex.withLock {
                previewableSource.startPreview(surface)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to bind video source", e)
    }
}

private suspend fun IPreviewableSource.stopAndResetPreview(shouldRelease: Boolean = false) {
    withContext(NonCancellable) {
        previewMutex.withLock {
            stopPreview()
            resetPreview()
            if (shouldRelease) {
                requestRelease()
            }
        }
    }
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
    trySend(Size(width, height))
    awaitClose {
        removeOnLayoutChangeListener(listener)
    }
}.filter { it.width > 0 && it.height > 0 }.distinctUntilChanged()
