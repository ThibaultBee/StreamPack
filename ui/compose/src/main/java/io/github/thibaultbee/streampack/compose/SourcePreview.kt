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

import android.util.Size
import android.view.SurfaceHolder
import androidx.camera.viewfinder.core.populateFromCharacteristics
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import io.github.thibaultbee.streampack.compose.utils.BitmapUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.bitmap.BitmapSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION_MS
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getCameraCharacteristics
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ui.views.ViewfinderView
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

private const val TAG = "ComposeSourcePreview"

/**
 * Displays the preview of a [IPreviewableSource].
 */
@Composable
fun SourcePreview(
    previewableSource: IPreviewableSource?,
    modifier: Modifier = Modifier,
    isTapToFocusEnabled: Boolean = true,
    onTapToFocus: ((Offset, Int) -> Unit)? = null,
    autoCancelDurationMillis: Long = DEFAULT_AUTO_CANCEL_DURATION_MS,
    isPinchToZoomEnabled: Boolean = true,
    onZoomRatioChanged: ((Float) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val viewfinderView = remember { mutableStateOf<ViewfinderView?>(null) }
    val viewSize = remember { mutableStateOf<Size?>(null) }
    val context = LocalContext.current

    DisposableEffect(previewableSource) {
        onDispose {
            scope.launch {
                previewableSource?.previewMutex?.withLock {
                    previewableSource.stopPreview()
                    previewableSource.resetPreview()
                }
            }
        }
    }

    LaunchedEffect(previewableSource, viewfinderView.value, viewSize.value) {
        val view = viewfinderView.value
        val size = viewSize.value
        if (view != null && size != null && size.width > 0 && size.height > 0) {
            try {
                previewableSource?.let { source ->
                    source.previewMutex.withLock {
                        source.stopPreview()
                        source.resetPreview()
                    }
                    val previewSize = source.getPreviewSize(size, SurfaceHolder::class.java)
                    val surface = view.requestSurface(previewSize) {
                        if (source is ICameraSource) {
                            val cameraCharacteristics = context.getCameraCharacteristics(source.cameraId)
                            populateFromCharacteristics(cameraCharacteristics)
                        } else {
                            val display = view.display
                            if (display != null) {
                                val rotationDegrees = OrientationUtils.getSurfaceRotationDegrees(display.rotation)
                                setSourceOrientation(rotationDegrees)
                            }
                        }
                    }
                    source.previewMutex.withLock {
                        source.startPreview(surface)
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set video source provider", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            ViewfinderView(ctx).apply {
                this.isPinchToZoomEnabled = isPinchToZoomEnabled
                this.isTapToFocusEnabled = isTapToFocusEnabled
                this.autoCancelDurationMillis = autoCancelDurationMillis
                this.onZoomRatioChanged = onZoomRatioChanged
                this.onTapToFocus = { pointF, rotation ->
                    onTapToFocus?.invoke(Offset(pointF.x, pointF.y), rotation)
                }
                viewfinderView.value = this
            }
        },
        update = { view ->
            view.isPinchToZoomEnabled = isPinchToZoomEnabled
            view.isTapToFocusEnabled = isTapToFocusEnabled
            view.autoCancelDurationMillis = autoCancelDurationMillis
            view.onZoomRatioChanged = onZoomRatioChanged
            view.onTapToFocus = { pointF, rotation ->
                onTapToFocus?.invoke(Offset(pointF.x, pointF.y), rotation)
            }
            viewfinderView.value = view
        },
        modifier = modifier.onSizeChanged { intSize ->
            viewSize.value = Size(intSize.width, intSize.height)
        },
        onRelease = { viewfinderView.value = null })
}

@Preview
@Composable
fun PreviewScreenSourcePreview() {
    val context = LocalContext.current
    val previewableSource = remember {
        BitmapSourceFactory(
            BitmapUtils.createImage(1280, 720)
        ).build(context) as IPreviewableSource
    }

    SourcePreview(previewableSource, modifier = Modifier.fillMaxSize())
}
