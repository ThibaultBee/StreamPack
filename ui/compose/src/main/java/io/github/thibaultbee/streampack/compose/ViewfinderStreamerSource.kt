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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.thibaultbee.streampack.compose.utils.BitmapUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.views.ViewfinderView
import io.github.thibaultbee.streampack.ui.views.bindToStreamerSource
import io.github.thibaultbee.streampack.ui.views.unbind

/**
 * Displays the preview of an [IWithVideoSource] (such as a Streamer) using [ViewfinderView].
 * It automatically observes the video source lifecycle and manages the preview surface.
 * It also handles pinch-to-zoom and tap-to-focus if the underlying source supports them.
 *
 * @param streamer The streamer to preview
 * @param modifier The modifier to apply to this composable
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 * @param onZoomChanged Hook called when the zoom ratio changes, providing the absolute zoom ratio
 */
@OptIn(ExperimentalStreamPackApi::class)
@Composable
fun ViewfinderStreamerSource(
    streamer: IWithVideoSource,
    modifier: Modifier = Modifier,
    isPinchToZoomEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true,
    onZoomChanged: ((Float) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            ViewfinderView(context).apply {
                bindToStreamerSource(
                    lifecycleOwner = lifecycleOwner,
                    streamer = streamer,
                    isPinchToZoomEnabled = isPinchToZoomEnabled,
                    isTapToFocusEnabled = isTapToFocusEnabled,
                    onZoomChanged = onZoomChanged
                )
            }
        },
        onRelease = { view ->
            view.unbind()
        },
        modifier = modifier
    )
}

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