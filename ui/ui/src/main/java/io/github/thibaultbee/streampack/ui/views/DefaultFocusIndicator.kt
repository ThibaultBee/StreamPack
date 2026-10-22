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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import io.github.thibaultbee.streampack.core.utils.ExperimentalStreamPackApi
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Default implementation of [FocusIndicator] displaying a square reticle at the touch point.
 *
 * It displays a white square while focusing, turns green when focus succeeds,
 * and turns red when focus fails, before fading out after [autoHideDuration].
 */
@ExperimentalStreamPackApi
class DefaultFocusIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), FocusIndicator {

    @VisibleForTesting
    internal enum class State {
        IDLE,
        FOCUSING,
        SUCCESS,
        FAILED
    }

    /**
     * The size of the focus square in pixels.
     */
    var boxSize: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        DEFAULT_BOX_SIZE_DP,
        resources.displayMetrics
    )
        set(value) {
            require(value > 0f) { "boxSize must be positive" }
            if (field == value) return
            field = value
            requestLayout()
            updatePosition()
            invalidate()
        }

    /**
     * The stroke width of the focus square in pixels.
     */
    var strokeWidth: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        DEFAULT_STROKE_WIDTH_DP,
        resources.displayMetrics
    )
        set(value) {
            require(value > 0f) { "strokeWidth must be positive" }
            if (field == value) return
            field = value
            paint.strokeWidth = value
            invalidate()
        }

    /**
     * The color of the square while focusing.
     */
    @get:ColorInt
    @setparam:ColorInt
    var focusingColor: Int = Color.WHITE
        set(value) {
            if (field == value) return
            field = value
            if (state == State.FOCUSING) {
                invalidate()
            }
        }

    /**
     * The color of the square when focus succeeds.
     */
    @get:ColorInt
    @setparam:ColorInt
    var successColor: Int = Color.GREEN
        set(value) {
            if (field == value) return
            field = value
            if (state == State.SUCCESS) {
                invalidate()
            }
        }

    /**
     * The color of the square when focus fails.
     */
    @get:ColorInt
    @setparam:ColorInt
    var failedColor: Int = Color.RED
        set(value) {
            if (field == value) return
            field = value
            if (state == State.FAILED) {
                invalidate()
            }
        }

    /**
     * Duration to keep the success or failure indicator visible before fading out.
     */
    var autoHideDuration: Duration = defaultAutoHideDuration
        set(value) {
            require(!value.isNegative()) { "autoHideDuration must not be negative" }
            field = value
        }

    /**
     * Optional bounds within the parent view to which this indicator is restricted.
     *
     * If not explicitly set and [parent] is an [InteractivePreviewView], the indicator will
     * automatically be clamped to stay within [InteractivePreviewView.contentRect].
     * If `null` and [parent] is not an [InteractivePreviewView], the indicator will be clamped
     * to the parent view bounds.
     */
    var contentBounds: RectF? = null
        set(value) {
            field = value
            updatePosition()
        }

    @VisibleForTesting
    internal var dynamicContentBounds: RectF? = null

    @VisibleForTesting
    internal var state: State = State.IDLE
        private set

    @VisibleForTesting
    internal var focusPoint: PointF? = null
        private set

    @VisibleForTesting
    internal val currentColor: Int
        get() = when (state) {
            State.IDLE, State.FOCUSING -> focusingColor
            State.SUCCESS -> successColor
            State.FAILED -> failedColor
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@DefaultFocusIndicator.strokeWidth
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scaleAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null
    private val autoHideRunnable = Runnable { fadeOut() }

    init {
        isClickable = false
        isFocusable = false
        if (isInEditMode) {
            state = State.FOCUSING
            visibility = VISIBLE
        } else {
            visibility = INVISIBLE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = ceil(boxSize).toInt().coerceAtLeast(1)
        setMeasuredDimension(
            resolveSize(desiredSize, widthMeasureSpec),
            resolveSize(desiredSize, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        pivotX = (right - left) / 2f
        pivotY = (bottom - top) / 2f
        updatePosition()
    }

    override fun onFocusStart(point: PointF) {
        onFocusStart(point, dynamicContentBounds)
    }

    override fun onFocusStart(point: PointF, bounds: RectF?) {
        cancelAnimations()

        dynamicContentBounds = bounds
        focusPoint = PointF(point.x, point.y)
        state = State.FOCUSING
        alpha = 1f
        scaleX = INITIAL_SCALE
        scaleY = INITIAL_SCALE
        updatePosition()
        visibility = VISIBLE
        invalidate()

        val animator = ValueAnimator.ofFloat(INITIAL_SCALE, 1f).apply {
            duration = SCALE_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                scaleX = scale
                scaleY = scale
            }
            addListener(object : AnimatorListenerAdapter() {
                private var isCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled && scaleAnimator === animation) {
                        scaleAnimator = null
                    }
                }
            })
        }
        scaleAnimator = animator
        animator.start()
    }

    override fun onFocusSuccess() {
        if (state != State.FOCUSING) return
        state = State.SUCCESS
        invalidate()
        scheduleAutoHide()
    }

    override fun onFocusFailed() {
        if (state != State.FOCUSING) return
        state = State.FAILED
        invalidate()
        scheduleAutoHide()
    }

    override fun onFocusCancel() {
        cancelAnimations()
        resetState()
    }

    private fun updatePosition() {
        val point = focusPoint ?: return
        val indicatorWidth = if (width > 0) width.toFloat() else boxSize
        val indicatorHeight = if (height > 0) height.toFloat() else boxSize

        var targetX = point.x - indicatorWidth / 2f
        var targetY = point.y - indicatorHeight / 2f

        val parentView = parent as? View
        if (parentView != null && parentView.width > 0 && parentView.height > 0) {
            val parentWidth = parentView.width.toFloat()
            val parentHeight = parentView.height.toFloat()
            val bounds = contentBounds ?: (parent as? InteractivePreviewView)?.contentRect ?: dynamicContentBounds

            val visibleLeft = (bounds?.left ?: 0f).coerceIn(0f, parentWidth)
            val visibleRight = (bounds?.right ?: parentWidth).coerceIn(visibleLeft, parentWidth)
            val visibleTop = (bounds?.top ?: 0f).coerceIn(0f, parentHeight)
            val visibleBottom = (bounds?.bottom ?: parentHeight).coerceIn(visibleTop, parentHeight)

            val maxX = (visibleRight - indicatorWidth).coerceAtLeast(visibleLeft)
            val maxY = (visibleBottom - indicatorHeight).coerceAtLeast(visibleTop)
            targetX = targetX.coerceIn(visibleLeft, maxX)
            targetY = targetY.coerceIn(visibleTop, maxY)
        }

        translationX = targetX - left
        translationY = targetY - top
    }

    private fun scheduleAutoHide() {
        mainHandler.removeCallbacks(autoHideRunnable)
        val delayMs = autoHideDuration.inWholeMilliseconds
        if (delayMs <= 0L) {
            fadeOut()
        } else {
            mainHandler.postDelayed(autoHideRunnable, delayMs)
        }
    }

    private fun fadeOut() {
        if (state == State.IDLE) return
        fadeAnimator?.let {
            fadeAnimator = null
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
        val animator = ValueAnimator.ofFloat(alpha, 0f).apply {
            duration = FADE_ANIMATION_DURATION_MS
            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                private var isCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled && fadeAnimator === animation) {
                        fadeAnimator = null
                        resetState()
                    }
                }
            })
        }
        fadeAnimator = animator
        animator.start()
    }

    private fun cancelAnimations() {
        mainHandler.removeCallbacks(autoHideRunnable)
        scaleAnimator?.let {
            scaleAnimator = null
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
        fadeAnimator?.let {
            fadeAnimator = null
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
    }

    private fun resetState() {
        state = State.IDLE
        focusPoint = null
        dynamicContentBounds = null
        visibility = INVISIBLE
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state == State.IDLE) return

        val halfStroke = strokeWidth / 2f
        val right = width - halfStroke
        val bottom = height - halfStroke
        if (right <= halfStroke || bottom <= halfStroke) return

        paint.color = currentColor
        paint.strokeWidth = strokeWidth
        canvas.drawRect(
            halfStroke,
            halfStroke,
            right,
            bottom,
            paint
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onFocusCancel()
    }

    companion object {
        private const val DEFAULT_BOX_SIZE_DP = 72f
        private const val DEFAULT_STROKE_WIDTH_DP = 2f
        private const val INITIAL_SCALE = 1.3f
        private const val SCALE_ANIMATION_DURATION_MS = 200L
        private const val FADE_ANIMATION_DURATION_MS = 200L

        /**
         * Default duration before hiding the indicator after focus completes.
         */
        val defaultAutoHideDuration: Duration = 1.seconds
    }
}
