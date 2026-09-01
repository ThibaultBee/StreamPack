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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.thibaultbee.streampack.compose.utils.BitmapUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.utils.bindToStreamerSource
import io.github.thibaultbee.streampack.ui.utils.unbind
import io.github.thibaultbee.streampack.ui.views.ViewfinderView
import kotlin.time.Duration

/**
 * Displays the preview of an [IWithVideoSource] (such as a Streamer) using [ViewfinderView].
 * It automatically observes the video source lifecycle and manages the preview surface.
 * It also handles pinch-to-zoom and tap-to-focus if the underlying source supports them.
 *
 * @param streamer The streamer to preview
 * @param modifier The modifier to apply to this composable
 * @param scaleMode The [ViewfinderView.ScaleMode] of the preview within its container
 * @param position The [ViewfinderView.Position] of the preview within its container
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 * @param autoCancelDuration The duration after which the focus area set by tap-to-focus is cleared
 * @param onPinchToZoom Listener called when a pinch-to-zoom gesture occurs on the view
 * @param onTapToFocus Listener called when a tap-to-focus gesture occurs on the view
 */
@ExperimentalStreamPackApi
@Composable
fun ViewfinderStreamerSource(
    streamer: IWithVideoSource,
    modifier: Modifier = Modifier,
    scaleMode: ViewfinderView.ScaleMode = ViewfinderView.ScaleMode.FILL,
    position: ViewfinderView.Position = ViewfinderView.Position.CENTER,
    isPinchToZoomEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true,
    autoCancelDuration: Duration = DEFAULT_AUTO_CANCEL_DURATION,
    onPinchToZoom: ViewfinderView.OnPinchToZoomListener? = null,
    onTapToFocus: ViewfinderView.OnTapToFocusListener? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var viewfinderView by remember { mutableStateOf<ViewfinderView?>(null) }

    DisposableEffect(
        viewfinderView,
        lifecycleOwner,
        streamer,
        isPinchToZoomEnabled,
        isTapToFocusEnabled
    ) {
        val view = viewfinderView ?: return@DisposableEffect onDispose {}
        view.bindToStreamerSource(
            lifecycleOwner = lifecycleOwner,
            streamer = streamer,
            isPinchToZoomEnabled = isPinchToZoomEnabled,
            isTapToFocusEnabled = isTapToFocusEnabled
        )
        onDispose {
            view.unbind()
        }
    }

    DisposableEffect(viewfinderView, onPinchToZoom) {
        val view = viewfinderView
        if (onPinchToZoom != null) {
            view?.addOnPinchToZoomListener(onPinchToZoom)
        }
        onDispose {
            if (onPinchToZoom != null) {
                view?.removeOnPinchToZoomListener(onPinchToZoom)
            }
        }
    }

    DisposableEffect(viewfinderView, onTapToFocus) {
        val view = viewfinderView
        if (onTapToFocus != null) {
            view?.addOnTapToFocusListener(onTapToFocus)
        }
        onDispose {
            if (onTapToFocus != null) {
                view?.removeOnTapToFocusListener(onTapToFocus)
            }
        }
    }

    AndroidView(
        factory = { context ->
            ViewfinderView(context).also {
                viewfinderView = it
            }
        },
        update = { view ->
            view.scaleMode = scaleMode
            view.position = position
            view.autoCancelDuration = autoCancelDuration
        },
        onRelease = {
            viewfinderView = null
        },
        modifier = modifier
    )
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