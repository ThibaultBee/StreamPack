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
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewControllableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.interfaces.IWithOutputs
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfaceDescriptor
import io.github.thibaultbee.streampack.core.pipelines.outputs.SurfacePipelineOutput
import io.github.thibaultbee.streampack.core.streamers.orientation.DisplayRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.IRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.rotationFlow
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.core.utils.InternalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import io.github.thibaultbee.streampack.ui.views.DefaultFocusIndicator
import io.github.thibaultbee.streampack.ui.views.FocusIndicator
import io.github.thibaultbee.streampack.ui.views.InteractivePreviewView
import io.github.thibaultbee.streampack.ui.views.SurfaceViewPreviewRenderer
import io.github.thibaultbee.streampack.ui.views.ViewfinderPreviewRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "InteractivePreviewViewExt"

/**
 * Binds the video source of a [IWithVideoSource] to an [InteractivePreviewView].
 *
 * The raw frames of the video source are displayed with an AndroidX `CameraViewfinder`, which
 * handles the rotation, the mirroring and the scaling of the frames.
 *
 * It automatically handles:
 * 1. Lifecycle-aware observing of the streamer's video source and configuration.
 * 2. Surface configuration of the preview.
 * 3. Gesture routing (pinch-to-zoom and tap-to-focus) to the underlying [IPreviewableSource].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe
 * @param streamer The streamer to preview
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 * @param focusIndicator The [FocusIndicator] notified during tap-to-focus, or `null` to disable.
 *   If it is an unattached [View] (such as the default [DefaultFocusIndicator]), it is automatically
 *   added to this [InteractivePreviewView] while tap-to-focus is active and removed when unbound.
 * @param contentOverlay An optional overlay [View] displayed over the video content, or `null` to disable.
 */
@ExperimentalStreamPackApi
fun InteractivePreviewView.bindToStreamerSource(
    lifecycleOwner: LifecycleOwner,
    streamer: IWithVideoSource,
    isPinchToZoomEnabled: Boolean = this.isPinchToZoomEnabled,
    isTapToFocusEnabled: Boolean = this.isTapToFocusEnabled,
    focusIndicator: FocusIndicator? = if (isTapToFocusEnabled) DefaultFocusIndicator(context) else null,
    contentOverlay: View? = null
) {
    this.isPinchToZoomEnabled = isPinchToZoomEnabled
    this.isTapToFocusEnabled = isTapToFocusEnabled
    if (contentOverlay != null) {
        this.contentOverlay = contentOverlay
    }
    val previousJob = getTag(R.id.preview_bind_job) as? Job
    previousJob?.cancel()

    val job = lifecycleOwner.lifecycleScope.launch {
        previousJob?.join()
        val viewfinderRenderer = ViewfinderPreviewRenderer(context)
        renderer = viewfinderRenderer
        try {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val contentRectJob = launch {
                    combine(
                        streamer.videoInput.sourceFlow,
                        streamer.videoInput.sourceConfigFlow,
                        layoutSizeFlow()
                    ) { _, _, _ ->
                        updateContentRect()
                    }.collect()
                }

                try {
                    streamer.videoInput.sourceFlow.collectLatest { source ->
                        val previewableSource = source as? IPreviewableSource ?: return@collectLatest

                        val detachGestures = (source as? IPreviewControllableSource)?.let {
                            attachGestures(
                                scope = this,
                                source = it,
                                focusIndicator = focusIndicator
                            )
                        }

                        try {
                            feedPreviewSource(viewfinderRenderer, streamer, previewableSource)
                        } finally {
                            detachGestures?.invoke()
                            previewableSource.stopAndResetPreview(
                                shouldRelease = streamer.videoInput.sourceFlow.value != source
                            )
                            viewfinderRenderer.releaseSurface()
                        }
                    }
                } finally {
                    contentRectJob.cancel()
                }
            }
        } finally {
            viewfinderRenderer.releaseSurface()
            if (renderer === viewfinderRenderer) {
                renderer = null
            }
        }
    }
    setTag(R.id.preview_bind_job, job)
}

/**
 * Binds the video output of a streamer pipeline to an [InteractivePreviewView].
 *
 * Instead of displaying the raw video source preview, this displays the processed
 * output of the streamer pipeline (including effects). The frames are rendered by the pipeline
 * into a [android.view.SurfaceView] of the size of the view, so the pipeline also applies the
 * rotation, the [InteractivePreviewView.scaleMode] and the [InteractivePreviewView.position].
 *
 * @param lifecycleOwner The [LifecycleOwner] to observe
 * @param streamer The streamer whose pipeline output is to be displayed
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 * @param focusIndicator The [FocusIndicator] notified during tap-to-focus, or `null` to disable.
 *   If it is an unattached [View] (such as the default [DefaultFocusIndicator]), it is automatically
 *   added to this [InteractivePreviewView] while tap-to-focus is active and removed when unbound.
 * @param rotationProvider The [IRotationProvider] providing display rotation. Defaults to [DisplayRotationProvider].
 * @param contentOverlay An optional overlay [View] displayed over the video content, or `null` to disable.
 */
@OptIn(InternalStreamPackApi::class)
@ExperimentalStreamPackApi
fun InteractivePreviewView.bindToStreamerOutput(
    lifecycleOwner: LifecycleOwner,
    streamer: IWithVideoSource,
    isPinchToZoomEnabled: Boolean = this.isPinchToZoomEnabled,
    isTapToFocusEnabled: Boolean = this.isTapToFocusEnabled,
    focusIndicator: FocusIndicator? = if (isTapToFocusEnabled) DefaultFocusIndicator(context) else null,
    rotationProvider: IRotationProvider = DisplayRotationProvider(context),
    contentOverlay: View? = null
) {
    this.isPinchToZoomEnabled = isPinchToZoomEnabled
    this.isTapToFocusEnabled = isTapToFocusEnabled
    if (contentOverlay != null) {
        this.contentOverlay = contentOverlay
    }
    require(streamer is IWithOutputs) { "Streamer must implement IWithOutputs" }

    val previousJob = getTag(R.id.preview_bind_job) as? Job
    previousJob?.cancel()

    val job = lifecycleOwner.lifecycleScope.launch {
        previousJob?.join()
        val surfaceRenderer = SurfaceViewPreviewRenderer(context) {
            streamer.getOutputContentSize(rotationProvider.rotation)
        }
        renderer = surfaceRenderer
        try {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val output = SurfacePipelineOutput(defaultRotation = rotationProvider.rotation)
                streamer.addOutput(output)

                val gestureJob = launch {
                    streamer.videoInput.sourceFlow.collectLatest { source ->
                        val controllableSource =
                            source as? IPreviewControllableSource ?: return@collectLatest

                        val detachGestures = attachGestures(
                            scope = this,
                            source = controllableSource,
                            focusIndicator = focusIndicator,
                            rotationProvider = { rotationProvider.rotation }
                        )

                        try {
                            awaitCancellation()
                        } finally {
                            detachGestures()
                        }
                    }
                }

                val streamJob = launch {
                    combine(
                        streamer.videoInput.sourceFlow,
                        streamer.videoInput.sourceConfigFlow,
                        output.surfaceFlow
                    ) { source, sourceConfig, surfaceDescriptor ->
                        source != null && sourceConfig != null && surfaceDescriptor != null && surfaceDescriptor.surface.isValid
                    }
                        .distinctUntilChanged()
                        .collect { canStream ->
                            try {
                                if (canStream) {
                                    output.startStream()
                                } else {
                                    output.stopStream()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (t: Throwable) {
                                Logger.e(
                                    TAG,
                                    "Failed to update output streaming state: ${t.message}",
                                    t
                                )
                            }
                        }
                }

                val contentRectJob = launch {
                    combine(
                        streamer.videoInput.sourceFlow,
                        streamer.videoInput.sourceConfigFlow,
                        rotationProvider.rotationFlow(),
                        layoutSizeFlow()
                    ) { _, _, _, _ ->
                        updateContentRect()
                    }.collect()
                }

                try {
                    feedPreviewOutput(surfaceRenderer, output, rotationProvider)
                } finally {
                    gestureJob.cancel()
                    streamJob.cancel()
                    contentRectJob.cancel()
                    withContext(NonCancellable) {
                        try {
                            output.stopStream()
                            output.setSurface(null)
                            streamer.removeOutput(output)
                            output.release()
                        } catch (t: Throwable) {
                            Logger.w(TAG, "Failed to clean up output: ${t.message}")
                        }
                    }
                }
            }
        } finally {
            surfaceRenderer.release()
            if (renderer === surfaceRenderer) {
                renderer = null
            }
        }
    }
    setTag(R.id.preview_bind_job, job)
}

/**
 * Unbinds the [InteractivePreviewView] from its currently bound [IWithVideoSource].
 *
 * This stops observing the video source, releases the preview surface,
 * and removes the internal tap and zoom gesture listeners added by [bindToStreamerSource] or
 * [bindToStreamerOutput].
 */
@ExperimentalStreamPackApi
fun InteractivePreviewView.unbind() {
    (getTag(R.id.preview_bind_job) as? Job)?.cancel()
    setTag(R.id.preview_bind_job, null)
}

/**
 * Attaches the pinch-to-zoom and tap-to-focus gestures that [source] supports.
 *
 * @return A function that detaches the gestures.
 */
@OptIn(ExperimentalStreamPackApi::class)
private fun InteractivePreviewView.attachGestures(
    scope: CoroutineScope,
    source: IPreviewControllableSource,
    focusIndicator: FocusIndicator?,
    rotationProvider: (() -> Int)? = null
): () -> Unit {
    val pinchToZoomListener =
        if (source.isZoomOnPinchSupported) {
            createPinchToZoomListener(scope, source).also {
                addOnPinchToZoomListener(it)
            }
        } else {
            null
        }

    val detachTapToFocus =
        if (source.isTapToFocusSupported) {
            attachTapToFocus(scope, source, focusIndicator, rotationProvider)
        } else {
            null
        }

    return {
        pinchToZoomListener?.let { removeOnPinchToZoomListener(it) }
        detachTapToFocus?.invoke()
    }
}

@OptIn(ExperimentalStreamPackApi::class)
private fun createPinchToZoomListener(
    scope: CoroutineScope,
    source: IPreviewControllableSource
) = InteractivePreviewView.OnPinchToZoomListener { scale, pinchCenter ->
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
private fun InteractivePreviewView.attachTapToFocus(
    scope: CoroutineScope,
    source: IPreviewControllableSource,
    focusIndicator: FocusIndicator?,
    rotationProvider: (() -> Int)? = null
): () -> Unit {
    val ownsIndicatorView = focusIndicator is View && focusIndicator.parent == null
    if (focusIndicator is View) {
        if (ownsIndicatorView) {
            addView(
                focusIndicator,
                focusIndicator.layoutParams ?: FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        } else if (focusIndicator.parent === this) {
            focusIndicator.bringToFront()
        }
    }

    val listener = createTapToFocusListener(
        scope = scope,
        source = source,
        focusIndicator = focusIndicator,
        rotationProvider = rotationProvider
    )
    addOnTapToFocusListener(listener)

    return {
        removeOnTapToFocusListener(listener)
        focusIndicator?.onFocusCancel()
        if (ownsIndicatorView && focusIndicator.parent === this) {
            removeView(focusIndicator)
        }
    }
}

@OptIn(ExperimentalStreamPackApi::class)
private fun InteractivePreviewView.createTapToFocusListener(
    scope: CoroutineScope,
    source: IPreviewControllableSource,
    focusIndicator: FocusIndicator?,
    rotationProvider: (() -> Int)? = null
): InteractivePreviewView.OnTapToFocusListener {
    var focusJob: Job? = null
    return InteractivePreviewView.OnTapToFocusListener { point, meteringPointFactory ->
        val previousJob = focusJob
        val displayRotation = rotationProvider?.invoke() ?: display?.rotation ?: Surface.ROTATION_0
        val rotationDegrees = OrientationUtils.getSurfaceRotationDegrees(displayRotation)
        focusJob = scope.launch {
            withContext(NonCancellable) {
                previousJob?.cancelAndJoin()
            }
            ensureActive()
            focusIndicator?.onFocusStart(point, contentRect)
            try {
                source.tapToFocus(
                    point,
                    meteringPointFactory,
                    rotationDegrees,
                    autoCancelDuration
                )
                focusIndicator?.onFocusSuccess()
            } catch (e: CancellationException) {
                focusIndicator?.onFocusCancel()
                throw e
            } catch (t: Throwable) {
                Logger.w(TAG, "Failed to focus at $point: ${t.message}")
                focusIndicator?.onFocusFailed()
            }
        }
    }
}

@OptIn(ExperimentalStreamPackApi::class)
private suspend fun InteractivePreviewView.feedPreviewSource(
    renderer: ViewfinderPreviewRenderer,
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
            startPreview(renderer, previewableSource, previewSize)
            requestLayout()
            updateContentRect()
        }
}

private suspend fun startPreview(
    renderer: ViewfinderPreviewRenderer,
    previewableSource: IPreviewableSource,
    previewSize: Size
) {
    try {
        previewableSource.stopAndResetPreview()

        val surface = renderer.requestSurface(
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

/**
 * Feeds the pipeline [output] with the surface of [renderer]: the pipeline renders frames at the
 * size of the view and applies the rotation, the scale mode and the position.
 */
@OptIn(ExperimentalStreamPackApi::class, InternalStreamPackApi::class)
private suspend fun InteractivePreviewView.feedPreviewOutput(
    renderer: SurfaceViewPreviewRenderer,
    output: SurfacePipelineOutput,
    rotationProvider: IRotationProvider
) {
    combine(
        renderer.surfaceFlow,
        scaleTypeFlow,
        rotationProvider.rotationFlow(),
        renderer.backgroundColorFlow
    ) { surfaceInfo, scaleType, rotation, backgroundColor ->
        surfaceInfo?.let {
            SurfaceDescriptor(
                surface = it.surface,
                resolution = it.size,
                scaleMode = InteractivePreviewView.getScaleMode(scaleType),
                position = InteractivePreviewView.getPosition(scaleType),
                targetRotation = rotation,
                backgroundColor = backgroundColor
            )
        }
    }
        .distinctUntilChanged()
        .collect { surfaceDescriptor ->
            output.setSurface(surfaceDescriptor?.takeIf { it.surface.isValid })
            requestLayout()
            updateContentRect()
        }
}

/**
 * Gets the size of the video rendered by the pipeline, as oriented on the display.
 *
 * It mirrors the transformation of the pipeline surface output: the source surface size, rotated by
 * the rotation relative to the display.
 */
private fun IWithVideoSource.getOutputContentSize(displayRotation: Int): Size? {
    val resolution = videoInput.sourceConfigFlow.value?.resolution ?: return null
    val infoProvider =
        (videoInput.sourceFlow.value as? IVideoSourceInternal)?.infoProviderFlow?.value
            ?: return resolution
    return infoProvider.getSurfaceSize(resolution).rotate(
        infoProvider.getRelativeRotationDegrees(displayRotation, false)
    )
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
