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

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.Surface
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest.Companion.MIRROR_MODE_HORIZONTAL
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION_MS
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import io.github.thibaultbee.streampack.ui.utils.PreviewConfigurationMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A [FrameLayout] containing a viewfinder preview.
 *
 * It handles the display, the aspect ratio and the scaling of the preview.
 * Touch and zoom gestures can be enabled and intercepted via callbacks.
 */
@ExperimentalStreamPackApi
class ViewfinderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val viewfinder = CameraViewfinder(context, attrs, defStyle)
    private var viewfinderSurfaceRequest: ViewfinderSurfaceRequest? = null

    var isPinchToZoomEnabled: Boolean = false
    var isTapToFocusEnabled: Boolean = false
    var autoCancelDurationMillis: Long = DEFAULT_AUTO_CANCEL_DURATION_MS

    var onTapToFocus: ((PointF, Int) -> Unit)? = null
    var onZoomRatioChanged: ((Float) -> Unit)? = null

    var position: Position
        get() = getPosition(viewfinder.scaleType)
        set(value) {
            viewfinder.scaleType = getScaleType(scaleMode, value)
        }

    var scaleMode: ScaleMode
        get() = getScaleMode(viewfinder.scaleType)
        set(value) {
            viewfinder.scaleType = getScaleType(value, position)
        }

    private var touchUpEvent: MotionEvent? = null
    private val pinchGesture = ScaleGestureDetector(context, PinchToZoomOnScaleGestureListener())

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)
        try {
            isPinchToZoomEnabled =
                a.getBoolean(R.styleable.ViewfinderView_isPinchToZoomEnabled, true)
            isTapToFocusEnabled = a.getBoolean(R.styleable.ViewfinderView_isTapToFocusEnabled, true)
            scaleMode = ScaleMode.entryOf(
                a.getInt(
                    R.styleable.ViewfinderView_scaleMode,
                    ScaleMode.FILL.value
                )
            )
            position = Position.entryOf(
                a.getInt(
                    R.styleable.ViewfinderView_position,
                    Position.CENTER.value
                )
            )
        } finally {
            a.recycle()
        }

        addView(
            viewfinder,
            ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    /**
     * Requests a [Surface] from the viewfinder with a specific size and configuration.
     * 
     * @param size The requested surface size.
     * @param block A builder block to configure the [ViewfinderSurfaceRequest.Builder].
     * @return The [Surface] provided by the viewfinder.
     */
    suspend fun requestSurface(
        size: Size,
        configuration: IPreviewableSource.PreviewConfiguration
    ): Surface {
        viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
        val builder = ViewfinderSurfaceRequest.Builder(size)
        configuration.orientationDegrees?.let {
            builder.setSourceOrientation(it)
        }

        configuration.implementationMode?.let {
            builder.setImplementationMode(
                PreviewConfigurationMapper.toViewfinderImplementationMode(it)
            )
        }

        if (configuration.isSourceMirroredHorizontally) {
            builder.setOutputMirrorMode(MIRROR_MODE_HORIZONTAL)
        }

        return withContext(Dispatchers.Main) {
            val request = builder.build().apply {
                viewfinderSurfaceRequest = this
            }
            viewfinder.requestSurface(request)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isPinchToZoomEnabled) {
            pinchGesture.onTouchEvent(event)
        }

        val isSingleTouch = event.pointerCount == 1
        val isUpEvent = event.action == MotionEvent.ACTION_UP
        val notALongPress =
            (event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout())
        if (isSingleTouch && isUpEvent && notALongPress) {
            // If the event is a click, invoke tap-to-focus and forward it to user's
            // OnClickListener#onClick.
            touchUpEvent = event
            performClick()
            // A click has been detected and forwarded. Consume the event so onClick won't be
            // invoked twice.
            return true
        }

        return true
    }

    override fun performClick(): Boolean {
        val x = touchUpEvent?.x ?: (width / 2f)
        val y = touchUpEvent?.y ?: (height / 2f)
        onTapToFocus?.invoke(
            PointF(x, y),
            OrientationUtils.getSurfaceRotationDegrees(display.rotation)
        )
        touchUpEvent = null
        return super.performClick()
    }

    private inner class PinchToZoomOnScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!isPinchToZoomEnabled) return false
            onZoomRatioChanged?.invoke(detector.scaleFactor)
            return true
        }
    }

    enum class Position(val value: Int) {
        START(0), CENTER(1), END(2);

        companion object {
            fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }

    enum class ScaleMode(val value: Int) {
        FILL(0), FIT(1);

        companion object {
            fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }

    companion object {
        private fun getPosition(scaleType: ScaleType): Position = when (scaleType) {
            ScaleType.FILL_START, ScaleType.FIT_START -> Position.START
            ScaleType.FILL_CENTER, ScaleType.FIT_CENTER -> Position.CENTER
            ScaleType.FILL_END, ScaleType.FIT_END -> Position.END
        }

        private fun getScaleMode(scaleType: ScaleType): ScaleMode = when (scaleType) {
            ScaleType.FILL_START, ScaleType.FILL_CENTER, ScaleType.FILL_END -> ScaleMode.FILL
            ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END -> ScaleMode.FIT
        }

        private fun getScaleType(scaleMode: ScaleMode, position: Position): ScaleType =
            when (position) {
                Position.START -> if (scaleMode == ScaleMode.FILL) ScaleType.FILL_START else ScaleType.FIT_START
                Position.CENTER -> if (scaleMode == ScaleMode.FILL) ScaleType.FILL_CENTER else ScaleType.FIT_CENTER
                Position.END -> if (scaleMode == ScaleMode.FILL) ScaleType.FILL_END else ScaleType.FIT_END
            }
    }
}
