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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import io.github.thibaultbee.streampack.compose.utils.BitmapUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION_MS
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.single.SingleStreamer
import io.github.thibaultbee.streampack.ui.views.PreviewView
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

private const val TAG = "ComposePreviewView"

/**
 * Displays the preview of a [IWithVideoSource].
 *
 * A [IWithVideoSource] must have a video sourc.
 *
 * @param videoSource the [IWithVideoSource] to preview
 * @param modifier the [Modifier] to apply to the [PreviewView]
 * @param enableZoomOnPinch enable zoom on pinch gesture
 * @param enableTapToFocus enable tap to focus
 * @param onTapToFocusTimeoutMs the duration in milliseconds after which the focus area set by tap-to-focus is cleared
 */
@Composable
fun PreviewScreen(
    videoSource: IWithVideoSource,
    modifier: Modifier = Modifier,
    enableZoomOnPinch: Boolean = true,
    enableTapToFocus: Boolean = true,
    onTapToFocusTimeoutMs: Long = DEFAULT_AUTO_CANCEL_DURATION_MS
) {
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.enableZoomOnPinch = enableZoomOnPinch
                this.enableTapToFocus = enableTapToFocus
                this.onTapToFocusTimeoutMs = onTapToFocusTimeoutMs

                scope.launch {
                    try {
                        setVideoSourceProvider(videoSource)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Failed to start preview", e)
                    }
                }
            }
        },
        modifier = modifier,
        onRelease = {
            scope.launch {
                val source = videoSource.videoInput?.sourceFlow?.value as? IPreviewableSource
                source?.previewMutex?.withLock {
                    source.stopPreview()
                    source.resetPreview()
                }
            }
        })
}

@Preview
@Composable
fun PreviewScreenPreview() {
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

    PreviewScreen(streamer, modifier = Modifier.fillMaxSize())
}