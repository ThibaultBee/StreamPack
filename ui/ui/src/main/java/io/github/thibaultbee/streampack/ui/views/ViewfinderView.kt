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
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.Surface
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest.Companion.MIRROR_MODE_HORIZONTAL
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION
import io.github.thibaultbee.streampack.core.elements.utils.MeteringPointFactory
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rotate
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import io.github.thibaultbee.streampack.ui.utils.PreviewConfigurationMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.time.Duration

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

    /**
     * The duration after which the focus area set by tap-to-focus is automatically cleared.
     */
    var autoCancelDuration: Duration = DEFAULT_AUTO_CANCEL_DURATION

    /**
     * Interface definition for a callback to be invoked when a tap-to-focus gesture occurs.
     */
    fun interface OnTapToFocusListener {
        /**
         * Called when a tap-to-focus gesture occurs.
         *
         * @param point The touch coordinates on the view.
         * @param meteringPointFactory The [MeteringPointFactory] to translate view coordinates to sensor/surface coordinates.
         */
        fun onTapToFocus(point: PointF, meteringPointFactory: MeteringPointFactory)
    }

    /**
     * Interface definition for a callback to be invoked when a pinch-to-zoom gesture occurs.
     */
    fun interface OnPinchToZoomListener {
        /**
         * Called when a pinch-to-zoom gesture occurs.
         *
         * @param scale The scale factor of the pinch gesture.
         * @param pinchCenter The center point of the pinch gesture on the view.
         */
        fun onPinchToZoom(scale: Float, pinchCenter: PointF)
    }

    private val tapToFocusListeners = CopyOnWriteArraySet<OnTapToFocusListener>()
    private val pinchToZoomListeners = CopyOnWriteArraySet<OnPinchToZoomListener>()

    /**
     * Adds an [OnTapToFocusListener] to be notified when a tap-to-focus gesture occurs.
     *
     * @param listener The listener to add.
     */
    fun addOnTapToFocusListener(listener: OnTapToFocusListener) {
        tapToFocusListeners.add(listener)
    }

    /**
     * Removes an [OnTapToFocusListener].
     *
     * @param listener The listener to remove.
     */
    fun removeOnTapToFocusListener(listener: OnTapToFocusListener) {
        tapToFocusListeners.remove(listener)
    }

    /**
     * Adds an [OnPinchToZoomListener] to be notified when a pinch-to-zoom gesture occurs.
     *
     * @param listener The listener to add.
     */
    fun addOnPinchToZoomListener(listener: OnPinchToZoomListener) {
        pinchToZoomListeners.add(listener)
    }

    /**
     * Removes an [OnPinchToZoomListener].
     *
     * @param listener The listener to remove.
     */
    fun removeOnPinchToZoomListener(listener: OnPinchToZoomListener) {
        pinchToZoomListeners.remove(listener)
    }

    private val meteringPointFactory = ViewMeteringPointFactory()

    private var scaleType: ScaleType = ScaleType.FIT_CENTER
        set(value) {
            viewfinder.scaleType = value
            field = value
        }

    /**
     * The position of the [ViewfinderView] within its container.
     */
    var position: Position
        get() = getPosition(scaleType)
        set(value) {
            scaleType = getScaleType(scaleMode, value)
        }

    /**
     * The scale mode of the [ViewfinderView] within its container.
     */
    var scaleMode: ScaleMode
        get() = getScaleMode(scaleType)
        set(value) {
            scaleType = getScaleType(value, position)
        }

    private var touchUpEvent: MotionEvent? = null
    private val pinchGesture = ScaleGestureDetector(context, PinchToZoomOnScaleGestureListener())

    /**
     * Gets a [Matrix] that transforms normalized video frame coordinates [0..1]
     * to the [PreviewView] coordinate space, accounting for the current [ScaleMode].
     *
     * This is highly useful for overlaying UI elements (like bounding boxes) on top of the
     * preview, or mapping UI touch events back to the video frame (via [Matrix.invert]).
     */
    internal val surfaceToViewMatrix: Matrix?
        get() {
            if (width == 0 || height == 0) {
                return null
            }
            val request = viewfinderSurfaceRequest
            val surfaceSize = request?.resolution

            if (surfaceSize == null || surfaceSize.width == 0 || surfaceSize.height == 0) {
                return Matrix().apply {
                    postScale(width.toFloat(), height.toFloat())
                }
            }
            val displayRotationDegrees =
                OrientationUtils.getSurfaceRotationDegrees(display?.rotation ?: Surface.ROTATION_0)
            val rotationDegrees =
                (request.sourceOrientation - displayRotationDegrees + 360) % 360
            val rotatedSurfaceSize = surfaceSize.rotate(rotationDegrees)
            return getSurfaceToViewMatrix(width, height, rotatedSurfaceSize, scaleType)
        }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)
        try {
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
     * @param configuration The preview configuration.
     * @return The [Surface] provided by the viewfinder.
     */
    suspend fun requestSurface(
        size: Size,
        configuration: IPreviewableSource.PreviewConfiguration
    ): Surface {
        val builder = ViewfinderSurfaceRequest.Builder(size)
        val orientationDegrees = configuration.orientationDegrees
            ?: OrientationUtils.getSurfaceRotationDegrees(display?.rotation ?: Surface.ROTATION_0)
        builder.setSourceOrientation(orientationDegrees)

        configuration.implementationMode?.let {
            builder.setImplementationMode(
                PreviewConfigurationMapper.toViewfinderImplementationMode(it)
            )
        }

        if (configuration.isSourceMirroredHorizontally) {
            builder.setOutputMirrorMode(MIRROR_MODE_HORIZONTAL)
        }

        return withContext(Dispatchers.Main) {
            viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
            val request = builder.build().apply {
                viewfinderSurfaceRequest = this
            }
            viewfinder.requestSurface(request)
        }
    }

    /**
     * Releases the current [Surface] requested via [requestSurface].
     */
    fun releaseSurface() {
        viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
        viewfinderSurfaceRequest = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (pinchToZoomListeners.isNotEmpty()) {
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
        if (tapToFocusListeners.isNotEmpty()) {
            val x = touchUpEvent?.x ?: (width / 2f)
            val y = touchUpEvent?.y ?: (height / 2f)
            val normalizedPoint = meteringPointFactory.createPoint(x, y)
            if (normalizedPoint.x in 0f..1f && normalizedPoint.y in 0f..1f) {
                val point = PointF(x, y)
                tapToFocusListeners.forEach {
                    it.onTapToFocus(point, meteringPointFactory)
                }
            }
        }
        touchUpEvent = null
        return super.performClick()
    }

    private inner class PinchToZoomOnScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (pinchToZoomListeners.isEmpty()) return false
            val pinchCenter = PointF(detector.focusX, detector.focusY)
            pinchToZoomListeners.forEach {
                it.onPinchToZoom(detector.scaleFactor, pinchCenter)
            }
            return true
        }
    }

    private inner class ViewMeteringPointFactory : MeteringPointFactory() {
        override fun translatePoint(x: Float, y: Float): PointF {
            if (width == 0 || height == 0) {
                return PointF(0f, 0f)
            }

            val matrix =
                surfaceToViewMatrix ?: return PointF(x / width.toFloat(), y / height.toFloat())
            val inverseMatrix = Matrix()
            matrix.invert(inverseMatrix)

            val point = floatArrayOf(x, y)
            inverseMatrix.mapPoints(point)

            return PointF(point[0], point[1])
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

        @VisibleForTesting
        internal fun getSurfaceToViewMatrix(
            viewWidth: Int,
            viewHeight: Int,
            surfaceSize: Size,
            scaleType: ScaleType
        ): Matrix {
            val surfaceWidth = surfaceSize.width.toFloat()
            val surfaceHeight = surfaceSize.height.toFloat()

            val surfaceRect = RectF(0f, 0f, surfaceWidth, surfaceHeight)
            val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            val matrix = Matrix()

            when (scaleType) {
                ScaleType.FIT_START -> matrix.setRectToRect(
                    surfaceRect,
                    viewRect,
                    Matrix.ScaleToFit.START
                )

                ScaleType.FIT_CENTER -> matrix.setRectToRect(
                    surfaceRect,
                    viewRect,
                    Matrix.ScaleToFit.CENTER
                )

                ScaleType.FIT_END -> matrix.setRectToRect(
                    surfaceRect,
                    viewRect,
                    Matrix.ScaleToFit.END
                )

                ScaleType.FILL_START -> {
                    matrix.setRectToRect(viewRect, surfaceRect, Matrix.ScaleToFit.START)
                    matrix.invert(matrix)
                }

                ScaleType.FILL_CENTER -> {
                    matrix.setRectToRect(viewRect, surfaceRect, Matrix.ScaleToFit.CENTER)
                    matrix.invert(matrix)
                }

                ScaleType.FILL_END -> {
                    matrix.setRectToRect(viewRect, surfaceRect, Matrix.ScaleToFit.END)
                    matrix.invert(matrix)
                }
            }

            matrix.preScale(surfaceWidth, surfaceHeight)
            return matrix
        }
    }
}
