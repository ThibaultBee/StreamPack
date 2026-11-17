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
package io.github.thibaultbee.streampack.compose

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.thibaultbee.streampack.compose.utils.BitmapUtils
import io.github.thibaultbee.streampack.compose.utils.toPosition
import io.github.thibaultbee.streampack.compose.utils.toScaleMode
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.defaultAutoCancelDuration
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.utils.bindToStreamerSource
import io.github.thibaultbee.streampack.ui.utils.unbind
import io.github.thibaultbee.streampack.ui.views.InteractivePreviewView
import kotlin.time.Duration

/**
 * Displays the preview of an [IWithVideoSource] (such as a Streamer) using [InteractivePreviewView].
 * It automatically observes the video source lifecycle and manages the preview surface.
 * It also handles pinch-to-zoom and tap-to-focus if the underlying source supports them.
 *
 * @param streamer The streamer to preview
 * @param modifier The modifier to apply to this composable
 * @param contentScale The [ContentScale] of the preview within its container
 * @param alignment The [Alignment] of the preview within its container
 * @param backgroundColor The background color of the preview
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 * @param autoCancelDuration The duration after which the focus area set by tap-to-focus is cleared
 * @param onPinchToZoom Listener called when a pinch-to-zoom gesture occurs on the view
 * @param onTapToFocus Listener called when a tap-to-focus gesture occurs on the view
 * @param focusIndicator Composable overlay displaying tap-to-focus feedback, or `null` to disable
 * @param contentOverlay Composable overlay displayed over the video content, or `null` to disable
 */
@ExperimentalStreamPackApi
@Composable
fun ViewfinderStreamerSource(
    streamer: IWithVideoSource,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    backgroundColor: Color = Color.Black,
    isPinchToZoomEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true,
    autoCancelDuration: Duration = defaultAutoCancelDuration,
    onPinchToZoom: InteractivePreviewView.OnPinchToZoomListener? = null,
    onTapToFocus: InteractivePreviewView.OnTapToFocusListener? = null,
    focusIndicator: (@Composable (FocusIndicatorState) -> Unit)? = { state ->
        DefaultFocusIndicator(state = state)
    },
    contentOverlay: (@Composable BoxScope.(contentBounds: Rect) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<InteractivePreviewView?>(null) }
    val focusIndicatorState = rememberFocusIndicatorState()
    val hasFocusIndicator = focusIndicator != null

    val contentRect by produceState<RectF?>(initialValue = null, previewView) {
        val view = previewView
        if (view != null) {
            view.contentRectFlow.collect { value = it }
        } else {
            value = null
        }
    }

    DisposableEffect(
        previewView,
        lifecycleOwner,
        streamer,
        hasFocusIndicator
    ) {
        val view = previewView ?: return@DisposableEffect onDispose {}
        view.bindToStreamerSource(
            lifecycleOwner = lifecycleOwner,
            streamer = streamer,
            isPinchToZoomEnabled = isPinchToZoomEnabled,
            isTapToFocusEnabled = isTapToFocusEnabled,
            focusIndicator = if (hasFocusIndicator) focusIndicatorState else null
        )
        onDispose {
            view.unbind()
        }
    }

    DisposableEffect(previewView, onPinchToZoom) {
        val view = previewView
        if (onPinchToZoom != null) {
            view?.addOnPinchToZoomListener(onPinchToZoom)
        }
        onDispose {
            if (onPinchToZoom != null) {
                view?.removeOnPinchToZoomListener(onPinchToZoom)
            }
        }
    }

    DisposableEffect(previewView, onTapToFocus) {
        val view = previewView
        if (onTapToFocus != null) {
            view?.addOnTapToFocusListener(onTapToFocus)
        }
        onDispose {
            if (onTapToFocus != null) {
                view?.removeOnTapToFocusListener(onTapToFocus)
            }
        }
    }

    Box(
        modifier = modifier
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InteractivePreviewView(context).also {
                    it.scaleMode = contentScale.toScaleMode()
                    it.position = alignment.toPosition()
                    it.previewBackgroundColor = backgroundColor.toArgb()
                    it.autoCancelDuration = autoCancelDuration
                    it.isPinchToZoomEnabled = isPinchToZoomEnabled
                    it.isTapToFocusEnabled = isTapToFocusEnabled
                    previewView = it
                }
            },
            update = { view ->
                view.scaleMode = contentScale.toScaleMode()
                view.position = alignment.toPosition()
                view.previewBackgroundColor = backgroundColor.toArgb()
                view.autoCancelDuration = autoCancelDuration
                view.isPinchToZoomEnabled = isPinchToZoomEnabled
                view.isTapToFocusEnabled = isTapToFocusEnabled
            },
            onRelease = {
                previewView = null
            }
        )

        if (contentOverlay != null) {
            val rect = contentRect
            if (rect != null && rect.width() > 0f && rect.height() > 0f) {
                val contentBounds = Rect(rect.left, rect.top, rect.right, rect.bottom)
                val density = LocalDensity.current
                val leftDp = with(density) { rect.left.toDp() }
                val topDp = with(density) { rect.top.toDp() }
                val widthDp = with(density) { rect.width().toDp() }
                val heightDp = with(density) { rect.height().toDp() }
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                        .offset(x = leftDp, y = topDp)
                        .size(width = widthDp, height = heightDp)
                ) {
                    contentOverlay(contentBounds)
                }
            }
        }

        if (focusIndicator != null) {
            Box(modifier = Modifier.matchParentSize()) {
                focusIndicator(focusIndicatorState)
            }
        }
    }
}

@OptIn(ExperimentalStreamPackApi::class)
@Preview
@Composable
fun PreviewScreenSourcePreview() {
    val context = LocalContext.current
    val streamer = SingleStreamer(context)
    LaunchedEffect(Unit) {
        streamer.setVideoSource(
            BitmapSourceFactory(
                BitmapUtils.createImage(
                    1280,
                    720
                )
            )
        )
    }

    ViewfinderStreamerSource(streamer, modifier = Modifier.fillMaxSize())
}
