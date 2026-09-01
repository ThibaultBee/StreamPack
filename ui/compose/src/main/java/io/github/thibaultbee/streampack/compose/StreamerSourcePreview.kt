package io.github.thibaultbee.streampack.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.views.ViewfinderView
import io.github.thibaultbee.streampack.ui.views.bindToStreamerSource

/**
 * Displays the preview of an [IWithVideoSource] (such as a Streamer) using [ViewfinderView].
 * It automatically observes the video source lifecycle and manages the preview surface.
 * It also handles pinch-to-zoom and tap-to-focus if the underlying source supports them.
 *
 * @param streamer The streamer to preview
 * @param modifier The modifier to apply to this composable
 * @param isPinchToZoomEnabled Whether pinch to zoom is enabled (if supported by the source)
 * @param isTapToFocusEnabled Whether tap to focus is enabled (if supported by the source)
 */
@OptIn(ExperimentalStreamPackApi::class)
@Composable
fun StreamerSourcePreview(
    streamer: IWithVideoSource,
    modifier: Modifier = Modifier,
    isPinchToZoomEnabled: Boolean = true,
    isTapToFocusEnabled: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            ViewfinderView(context).apply {
                bindToStreamerSource(
                    lifecycleOwner,
                    streamer,
                    isPinchToZoomEnabled = isPinchToZoomEnabled,
                    isTapToFocusEnabled = isTapToFocusEnabled
                )
            }
        },
        modifier = modifier
    )
}