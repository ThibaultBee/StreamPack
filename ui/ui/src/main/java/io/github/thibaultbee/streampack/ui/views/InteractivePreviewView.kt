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
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.camera.viewfinder.core.ScaleType
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.defaultAutoCancelDuration
import io.github.thibaultbee.streampack.core.elements.utils.MeteringPointFactory
import io.github.thibaultbee.streampack.core.elements.utils.Position
import io.github.thibaultbee.streampack.core.elements.utils.ScaleMode
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import io.github.thibaultbee.streampack.ui.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * A [FrameLayout] that displays a video preview and handles user interactions on it.
 *
 * The view does not render frames by itself: rendering is delegated to an internal renderer
 * child view, attached when the view is bound to a streamer:
 * - `bindToStreamerSource` displays the raw video source with an AndroidX `CameraViewfinder`.
 * - `bindToStreamerOutput` displays the processed pipeline output with a
 *   [android.view.SurfaceView].
 *
 * Whatever the renderer, the view provides the same features: scaling ([scaleMode] and
 * [position]), pinch-to-zoom and tap-to-focus. Touches outside the displayed video
 * (for example, in the bars of [ScaleMode.FIT]) are ignored.
 */
@ExperimentalStreamPackApi
class InteractivePreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    /**
     * The duration after which the focus area set by tap-to-focus is automatically cleared.
     */
    var autoCancelDuration: Duration = defaultAutoCancelDuration

    /**
     * Whether pinch-to-zoom gesture is enabled.
     *
     * Defaults to `true`.
     */
    var isPinchToZoomEnabled: Boolean = true

    /**
     * Whether tap-to-focus gesture is enabled.
     *
     * Defaults to `true`.
     */
    var isTapToFocusEnabled: Boolean = true

    /**
     * Interface definition for a callback to be invoked when a tap-to-focus gesture occurs.
     */
    fun interface OnTapToFocusListener {
        /**
         * Called when a tap-to-focus gesture occurs.
         *
         * @param point The touch coordinates on the view.
         * @param meteringPointFactory The [MeteringPointFactory] to translate view coordinates to normalized coordinates of the displayed video.
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

    /**
     * Interface definition for a callback to be invoked when the bounds of the displayed video content change.
     */
    fun interface OnContentRectChangedListener {
        /**
         * Called when the content bounds change.
         *
         * @param contentRect The new bounds of the displayed video content in view coordinates, or `null` if unknown.
         */
        fun onContentRectChanged(contentRect: RectF?)
    }

    private val tapToFocusListeners = CopyOnWriteArraySet<OnTapToFocusListener>()
    private val pinchToZoomListeners = CopyOnWriteArraySet<OnPinchToZoomListener>()
    private val contentRectChangedListeners = CopyOnWriteArraySet<OnContentRectChangedListener>()

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

    /**
     * Adds an [OnContentRectChangedListener] to be notified when the displayed content bounds change.
     *
     * @param listener The listener to add.
     */
    fun addOnContentRectChangedListener(listener: OnContentRectChangedListener) {
        contentRectChangedListeners.add(listener)
        listener.onContentRectChanged(contentRect)
    }

    /**
     * Removes an [OnContentRectChangedListener].
     *
     * @param listener The listener to remove.
     */
    fun removeOnContentRectChangedListener(listener: OnContentRectChangedListener) {
        contentRectChangedListeners.remove(listener)
    }

    private val meteringPointFactory = ViewMeteringPointFactory()

    private val _scaleTypeFlow = MutableStateFlow(ScaleType.FILL_CENTER)
    internal val scaleTypeFlow: StateFlow<ScaleType> = _scaleTypeFlow.asStateFlow()

    private var scaleType: ScaleType
        get() = _scaleTypeFlow.value
        set(value) {
            if (_scaleTypeFlow.value == value) return
            _scaleTypeFlow.value = value
            renderer?.setScaleType(value)
            requestLayout()
            updateContentRect()
        }

    /**
     * The position of the preview within the [InteractivePreviewView].
     */
    var position: Position
        get() = getPosition(scaleType)
        set(value) {
            scaleType = getScaleType(scaleMode, value)
        }

    /**
     * The scale mode of the preview within the [InteractivePreviewView].
     */
    var scaleMode: ScaleMode
        get() = getScaleMode(scaleType)
        set(value) {
            scaleType = getScaleType(value, position)
        }

    /**
     * The background color of the preview.
     *
     * This color is used for the letterbox and pillarbox bars when [scaleMode] is [ScaleMode.FIT].
     *
     * Defaults to [Color.BLACK].
     */
    @get:ColorInt
    @setparam:ColorInt
    var previewBackgroundColor: Int = Color.BLACK
        set(value) {
            if (field == value) return
            field = value
            renderer?.setBackgroundColor(value)
        }

    override fun setBackgroundColor(@ColorInt color: Int) {
        super.setBackgroundColor(color)
        previewBackgroundColor = color
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background)
        if (background is ColorDrawable) {
            previewBackgroundColor = background.color
        }
    }

    /**
     * An optional overlay [View] displayed on top of the preview frames and positioned over
     * the displayed video content.
     *
     * The overlay is measured and positioned to match [contentRect]. If [contentRect] is `null`
     * (the preview is not ready or has not been laid out), the overlay is not displayed.
     */
    var contentOverlay: View? = null
        set(value) {
            if (field === value) {
                return
            }
            field?.let {
                if (it.parent === this) {
                    removeView(it)
                }
            }
            field = value
            value?.let {
                if (it.parent !== this) {
                    val index = if (renderer != null) 1 else 0
                    addView(it, index)
                }
                requestLayout()
            }
        }

    /**
     * The renderer that displays the frames, or `null` if the view is not bound.
     *
     * Its view is always kept below the other children (such as a focus indicator).
     * The caller that sets a renderer is responsible for releasing it.
     */
    internal var renderer: PreviewRenderer? = null
        set(value) {
            if (field === value) {
                return
            }
            field?.let { removeView(it.view) }
            field = value
            value?.let {
                it.setScaleType(scaleType)
                it.setBackgroundColor(previewBackgroundColor)
                addView(it.view, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
            requestLayout()
            updateContentRect()
        }

    private var touchUpEvent: MotionEvent? = null
    private val pinchGesture = ScaleGestureDetector(context, PinchToZoomOnScaleGestureListener())

    private val _contentRectFlow = MutableStateFlow<RectF?>(null)

    /**
     * A [StateFlow] that emits the current bounds of the displayed video content in view
     * coordinates, or `null` if the displayed video is unknown or the view has not been laid out.
     */
    val contentRectFlow: StateFlow<RectF?> = _contentRectFlow.asStateFlow()

    internal fun updateContentRect() {
        val rect = contentRect
        if (_contentRectFlow.value != rect) {
            _contentRectFlow.value = rect
            contentRectChangedListeners.forEach { it.onContentRectChanged(rect) }
            contentOverlay?.let { overlay ->
                if (rect != null) {
                    overlay.layout(
                        rect.left.roundToInt(),
                        rect.top.roundToInt(),
                        rect.right.roundToInt(),
                        rect.bottom.roundToInt()
                    )
                }
            }
        }
    }

    /**
     * Gets a [Matrix] that transforms normalized video coordinates [0..1] to the view
     * coordinate space, or `null` if the displayed video is unknown.
     */
    internal val contentToViewMatrix: Matrix?
        get() {
            if (width == 0 || height == 0) {
                return null
            }
            return renderer?.getContentToViewMatrix(width, height, scaleType)
        }

    /**
     * Gets the bounds of the displayed video content in view coordinates, or `null` if
     * the displayed video is unknown or the view has not been laid out.
     */
    val contentRect: RectF?
        get() {
            val matrix = contentToViewMatrix ?: return null
            val rect = RectF(0f, 0f, 1f, 1f)
            matrix.mapRect(rect)
            return rect
        }

    private fun isPointInContent(x: Float, y: Float): Boolean {
        if (x !in 0f..width.toFloat() || y !in 0f..height.toFloat()) {
            return false
        }
        val rect = contentRect ?: return true
        return x in rect.left..rect.right && y in rect.top..rect.bottom
    }

    private var contentOverlayId: Int = View.NO_ID

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.InteractivePreviewView)
        try {
            scaleMode = ScaleMode.entryOf(
                a.getInt(
                    R.styleable.InteractivePreviewView_scaleMode,
                    ScaleMode.CROP.value
                )
            )
            position = Position.entryOf(
                a.getInt(
                    R.styleable.InteractivePreviewView_position,
                    Position.CENTER.value
                )
            )
            contentOverlayId = a.getResourceId(
                R.styleable.InteractivePreviewView_contentOverlay,
                View.NO_ID
            )
            if (a.hasValue(R.styleable.InteractivePreviewView_previewBackgroundColor)) {
                previewBackgroundColor = a.getColor(
                    R.styleable.InteractivePreviewView_previewBackgroundColor,
                    Color.BLACK
                )
            } else if (background is ColorDrawable) {
                previewBackgroundColor = (background as ColorDrawable).color
            }
            isPinchToZoomEnabled = a.getBoolean(
                R.styleable.InteractivePreviewView_isPinchToZoomEnabled,
                true
            )
            isTapToFocusEnabled = a.getBoolean(
                R.styleable.InteractivePreviewView_isTapToFocusEnabled,
                true
            )
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (contentOverlayId != View.NO_ID) {
            contentOverlay = findViewById(contentOverlayId)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val overlay = contentOverlay ?: return
        val matrix = renderer?.getContentToViewMatrix(measuredWidth, measuredHeight, scaleType)
        if (matrix != null) {
            val rect = RectF(0f, 0f, 1f, 1f)
            matrix.mapRect(rect)
            val overlayWidth = rect.width().roundToInt().coerceAtLeast(0)
            val overlayHeight = rect.height().roundToInt().coerceAtLeast(0)
            overlay.measure(
                MeasureSpec.makeMeasureSpec(overlayWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(overlayHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateContentRect()
        contentOverlay?.let { overlay ->
            val rect = contentRect
            if (rect != null) {
                overlay.layout(
                    rect.left.roundToInt(),
                    rect.top.roundToInt(),
                    rect.right.roundToInt(),
                    rect.bottom.roundToInt()
                )
            } else {
                overlay.layout(0, 0, 0, 0)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !isPointInContent(event.x, event.y)) {
            return false
        }

        if (isPinchToZoomEnabled && pinchToZoomListeners.isNotEmpty()) {
            pinchGesture.onTouchEvent(event)
        }

        val isSingleTouch = event.pointerCount == 1
        val isUpEvent = event.action == MotionEvent.ACTION_UP
        val notALongPress =
            (event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout())
        if (isSingleTouch && isUpEvent && notALongPress) {
            if (!isPointInContent(event.x, event.y)) {
                return false
            }
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
        if (isTapToFocusEnabled && tapToFocusListeners.isNotEmpty()) {
            val x = touchUpEvent?.x ?: (width / 2f)
            val y = touchUpEvent?.y ?: (height / 2f)
            if (isPointInContent(x, y)) {
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
            if (pinchToZoomListeners.isEmpty() || !isPointInContent(detector.focusX, detector.focusY)) {
                return false
            }
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
                contentToViewMatrix ?: return PointF(x / width.toFloat(), y / height.toFloat())
            val inverseMatrix = Matrix()
            matrix.invert(inverseMatrix)

            val point = floatArrayOf(x, y)
            inverseMatrix.mapPoints(point)

            return PointF(point[0], point[1])
        }
    }

    companion object {
        internal fun getPosition(scaleType: ScaleType): Position = when (scaleType) {
            ScaleType.FILL_START, ScaleType.FIT_START -> Position.START
            ScaleType.FILL_CENTER, ScaleType.FIT_CENTER -> Position.CENTER
            ScaleType.FILL_END, ScaleType.FIT_END -> Position.END
        }

        internal fun getScaleMode(scaleType: ScaleType): ScaleMode = when (scaleType) {
            ScaleType.FILL_START, ScaleType.FILL_CENTER, ScaleType.FILL_END -> ScaleMode.CROP
            ScaleType.FIT_START, ScaleType.FIT_CENTER, ScaleType.FIT_END -> ScaleMode.FIT
        }

        internal fun getScaleType(scaleMode: ScaleMode, position: Position): ScaleType =
            when (position) {
                Position.START -> if (scaleMode == ScaleMode.CROP) ScaleType.FILL_START else ScaleType.FIT_START
                Position.CENTER -> if (scaleMode == ScaleMode.CROP) ScaleType.FILL_CENTER else ScaleType.FIT_CENTER
                Position.END -> if (scaleMode == ScaleMode.CROP) ScaleType.FILL_END else ScaleType.FIT_END
            }

        /**
         * Gets a [Matrix] that transforms normalized content coordinates [0..1] to the view
         * coordinate space, for a content of [contentSize] displayed with [scaleType].
         *
         * @param viewWidth The view width in pixels.
         * @param viewHeight The view height in pixels.
         * @param contentSize The size of the displayed content, as oriented on the display.
         * @param scaleType The [ScaleType] of the content within the view.
         */
        internal fun getContentToViewMatrix(
            viewWidth: Int,
            viewHeight: Int,
            contentSize: Size,
            scaleType: ScaleType
        ): Matrix {
            val contentWidth = contentSize.width.toFloat()
            val contentHeight = contentSize.height.toFloat()

            val contentRect = RectF(0f, 0f, contentWidth, contentHeight)
            val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            val matrix = Matrix()

            when (scaleType) {
                ScaleType.FIT_START -> matrix.setRectToRect(
                    contentRect,
                    viewRect,
                    Matrix.ScaleToFit.START
                )

                ScaleType.FIT_CENTER -> matrix.setRectToRect(
                    contentRect,
                    viewRect,
                    Matrix.ScaleToFit.CENTER
                )

                ScaleType.FIT_END -> matrix.setRectToRect(
                    contentRect,
                    viewRect,
                    Matrix.ScaleToFit.END
                )

                ScaleType.FILL_START -> {
                    matrix.setRectToRect(viewRect, contentRect, Matrix.ScaleToFit.START)
                    matrix.invert(matrix)
                }

                ScaleType.FILL_CENTER -> {
                    matrix.setRectToRect(viewRect, contentRect, Matrix.ScaleToFit.CENTER)
                    matrix.invert(matrix)
                }

                ScaleType.FILL_END -> {
                    matrix.setRectToRect(viewRect, contentRect, Matrix.ScaleToFit.END)
                    matrix.invert(matrix)
                }
            }

            matrix.preScale(contentWidth, contentHeight)
            return matrix
        }
    }
}
