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

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.views.DefaultFocusIndicator.Companion.defaultAutoHideDuration
import io.github.thibaultbee.streampack.ui.views.FocusIndicator
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * State holder implementing [FocusIndicator] to drive a Compose tap-to-focus overlay.
 */
@ExperimentalStreamPackApi
@Stable
class FocusIndicatorState : FocusIndicator {
    enum class Status {
        IDLE,
        FOCUSING,
        SUCCESS,
        FAILED
    }

    /**
     * Current status of the tap-to-focus operation.
     */
    var status by mutableStateOf(Status.IDLE)
        private set

    /**
     * Touch coordinates in pixels within the viewfinder, or [Offset.Unspecified] when [Status.IDLE].
     */
    var tapOffset by mutableStateOf(Offset.Unspecified)
        private set

    /**
     * Bounds of the displayed content in pixels within the viewfinder, or `null` if unknown.
     */
    var contentBounds by mutableStateOf<Rect?>(null)
        private set

    /**
     * Monotonically increasing identifier incremented on every [onFocusStart] call.
     */
    var focusRequestId by mutableLongStateOf(0L)
        private set

    override fun onFocusStart(point: PointF) {
        onFocusStart(point, null)
    }

    override fun onFocusStart(point: PointF, bounds: RectF?) {
        focusRequestId++
        tapOffset = Offset(point.x, point.y)
        contentBounds = bounds?.let { Rect(it.left, it.top, it.right, it.bottom) }
        status = Status.FOCUSING
    }

    override fun onFocusSuccess() {
        if (status != Status.FOCUSING) return
        status = Status.SUCCESS
    }

    override fun onFocusFailed() {
        if (status != Status.FOCUSING) return
        status = Status.FAILED
    }

    override fun onFocusCancel() {
        status = Status.IDLE
        tapOffset = Offset.Unspecified
        contentBounds = null
    }
}

/**
 * Remembers a [FocusIndicatorState] instance across compositions.
 */
@ExperimentalStreamPackApi
@Composable
fun rememberFocusIndicatorState(): FocusIndicatorState = remember { FocusIndicatorState() }

/**
 * Default Compose focus indicator displaying a square reticle at the tap point.
 *
 * Displays a white square while focusing, turns green when focus succeeds,
 * and turns red when focus fails, before fading out after [autoHideDuration].
 *
 * @param state The [FocusIndicatorState] driving this indicator
 * @param modifier The modifier to apply to the indicator container
 * @param boxSize The size of the focus square
 * @param strokeWidth The stroke width of the focus square
 * @param focusingColor The color of the square while focusing
 * @param successColor The color of the square when focus succeeds
 * @param failedColor The color of the square when focus fails
 * @param autoHideDuration Duration to keep the success or failure indicator visible before fading out
 */
@ExperimentalStreamPackApi
@Composable
fun DefaultFocusIndicator(
    state: FocusIndicatorState,
    modifier: Modifier = Modifier,
    boxSize: Dp = 72.dp,
    strokeWidth: Dp = 2.dp,
    focusingColor: Color = Color.White,
    successColor: Color = Color.Green,
    failedColor: Color = Color.Red,
    autoHideDuration: Duration = defaultAutoHideDuration
) {
    val isInspectionMode = LocalInspectionMode.current
    val scale = remember { Animatable(if (isInspectionMode) 1f else INITIAL_SCALE) }
    val alpha = remember { Animatable(1f) }
    var hiddenRequestId by remember { mutableLongStateOf(0L) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.focusRequestId) {
        if (state.focusRequestId > 0L && state.status != FocusIndicatorState.Status.IDLE) {
            scale.snapTo(INITIAL_SCALE)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SCALE_ANIMATION_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    LaunchedEffect(state.focusRequestId, state.status) {
        when (state.status) {
            FocusIndicatorState.Status.IDLE -> {
                alpha.snapTo(1f)
            }

            FocusIndicatorState.Status.FOCUSING -> {
                alpha.snapTo(1f)
            }

            FocusIndicatorState.Status.SUCCESS,
            FocusIndicatorState.Status.FAILED -> {
                val requestId = state.focusRequestId
                alpha.snapTo(1f)
                delay(autoHideDuration)
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = FADE_ANIMATION_DURATION_MS)
                )
                hiddenRequestId = requestId
            }
        }
    }

    val isVisible = state.status != FocusIndicatorState.Status.IDLE &&
        state.tapOffset != Offset.Unspecified &&
        hiddenRequestId != state.focusRequestId

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        if (isVisible) {
            val density = LocalDensity.current
            val boxSizePx = with(density) { boxSize.toPx() }
            val color = when (state.status) {
                FocusIndicatorState.Status.IDLE,
                FocusIndicatorState.Status.FOCUSING -> focusingColor
                FocusIndicatorState.Status.SUCCESS -> successColor
                FocusIndicatorState.Status.FAILED -> failedColor
            }

            Canvas(
                modifier = Modifier
                    .offset {
                        val tap = state.tapOffset
                        var targetX = tap.x - boxSizePx / 2f
                        var targetY = tap.y - boxSizePx / 2f
                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val containerWidth = containerSize.width.toFloat()
                            val containerHeight = containerSize.height.toFloat()
                            val bounds = state.contentBounds

                            val visibleLeft = (bounds?.left ?: 0f).coerceIn(0f, containerWidth)
                            val visibleRight = (bounds?.right ?: containerWidth).coerceIn(visibleLeft, containerWidth)
                            val visibleTop = (bounds?.top ?: 0f).coerceIn(0f, containerHeight)
                            val visibleBottom = (bounds?.bottom ?: containerHeight).coerceIn(visibleTop, containerHeight)

                            val maxX = (visibleRight - boxSizePx).coerceAtLeast(visibleLeft)
                            val maxY = (visibleBottom - boxSizePx).coerceAtLeast(visibleTop)
                            targetX = targetX.coerceIn(visibleLeft, maxX)
                            targetY = targetY.coerceIn(visibleTop, maxY)
                        }
                        IntOffset(targetX.roundToInt(), targetY.roundToInt())
                    }
                    .size(boxSize)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
            ) {
                val strokePx = strokeWidth.toPx()
                val halfStroke = strokePx / 2f
                val rectWidth = size.width - strokePx
                val rectHeight = size.height - strokePx
                if (rectWidth > 0f && rectHeight > 0f) {
                    drawRect(
                        color = color,
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(rectWidth, rectHeight),
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }
    }
}

private const val INITIAL_SCALE = 1.3f
private const val SCALE_ANIMATION_DURATION_MS = 200
private const val FADE_ANIMATION_DURATION_MS = 200

@OptIn(ExperimentalStreamPackApi::class)
private class FocusStatusPreviewParameterProvider :
    PreviewParameterProvider<FocusIndicatorState.Status> {
    override val values = sequenceOf(
        FocusIndicatorState.Status.FOCUSING,
        FocusIndicatorState.Status.SUCCESS,
        FocusIndicatorState.Status.FAILED
    )
}

@OptIn(ExperimentalStreamPackApi::class)
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DefaultFocusIndicatorPreview(
    @PreviewParameter(FocusStatusPreviewParameterProvider::class)
    status: FocusIndicatorState.Status
) {
    val previewSize = 120.dp
    val centerPx = with(LocalDensity.current) { (previewSize / 2).toPx() }
    val state = remember(status, centerPx) {
        FocusIndicatorState().apply {
            onFocusStart(PointF(centerPx, centerPx))
            when (status) {
                FocusIndicatorState.Status.SUCCESS -> onFocusSuccess()
                FocusIndicatorState.Status.FAILED -> onFocusFailed()
                else -> Unit
            }
        }
    }
    DefaultFocusIndicator(
        state = state,
        modifier = Modifier.size(previewSize)
    )
}
