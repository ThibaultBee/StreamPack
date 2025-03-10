/*
 * Copyright 2022 Thibault B.
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

import android.Manifest
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.getCameraCharacteristics
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.runningHistory
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCallbackStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraCoroutineStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.IVideoStreamer
import io.github.thibaultbee.streampack.core.streamers.interfaces.startPreview
import io.github.thibaultbee.streampack.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.InvalidParameterException
import java.util.concurrent.CancellationException

/**
 * A [FrameLayout] containing a preview for the [ICameraStreamer].
 *
 * It handles the display, the aspect ratio and the scaling of the preview.
 *
 * In the case, you are using it, do not call [ICameraCoroutineStreamer.startPreview] (or
 * [ICameraCallbackStreamer.stopPreview]) and [ICameraCoroutineStreamer.stopPreview] on application
 * side. It will be handled by the [PreviewView].
 *
 * The [Manifest.permission.CAMERA] permission must be granted before using this view.
 */
class PreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val cameraViewfinder = CameraViewfinder(context, attrs, defStyle)

    private var viewfinderSurfaceRequest: ViewfinderSurfaceRequest? = null

    private val lifecycleScope: CoroutineScope?
        get() = findViewTreeLifecycleOwner()?.lifecycleScope
    private var sourceFlowJob: Job? = null

    /**
     * Whether the preview is running.
     */
    val isPreviewing: Boolean
        get() {
            val videoSource = streamer?.videoSourceFlow?.value
            return videoSource is IVideoPreviewableSource && videoSource.isPreviewingFlow.value
        }

    /**
     * Enables zoom on pinch gesture.
     */
    var enableZoomOnPinch: Boolean

    /**
     * Enables tap to focus.
     */
    var enableTapToFocus: Boolean

    /**
     * Sets the [IVideoStreamer] to preview.
     * To force the preview to start, use [startPreviewAsync] or [startPreview].
     */
    var streamer: IVideoStreamer? = null
        /**
         * Sets the [IVideoStreamer] to preview.
         *
         * If the previous streamer was previewing, it will stop the preview and start the new one.
         * If the new streamer is already previewing, it will throw an exception. Make sure to stop
         * the preview before setting a new streamer.
         *
         * @param newStreamer the [IVideoStreamer] to preview
         */
        set(newStreamer) {
            if (field == newStreamer) {
                Logger.w(TAG, "No need to set the same video streamer")
                return
            }
            val newSource = newStreamer?.videoSourceFlow?.value as? IVideoPreviewableSource
            if (newSource != null) {
                require(!newSource.isPreviewingFlow.value) { "Cannot set streamer while it is already previewing. Stop preview before." }
            }

            val previousSource = field?.videoSourceFlow?.value as? IVideoPreviewableSource
            val isPreviousSourcePreviewing = previousSource?.isPreviewingFlow?.value ?: false

            sourceFlowJob?.cancel()
            sourceFlowJob = null
            lifecycleScope?.let {
                if (isPreviousSourcePreviewing) {
                    it.launch {
                        stopPreviewInternal()
                    }
                }

                newStreamer?.let { streamer ->
                    registerStreamer(streamer)
                }
            } ?: Logger.e(TAG, "Trying to set streamer but lifecycleScope is not available")

            field = newStreamer
        }

    /**
     * The position of the [PreviewView] within its container.
     */
    var position: Position
        get() = getPosition(cameraViewfinder.scaleType)
        set(value) {
            cameraViewfinder.scaleType = getScaleType(scaleMode, value)
        }

    /**
     * The scale mode of the [PreviewView] within its container.
     */
    var scaleMode: ScaleMode
        get() = getScaleMode(cameraViewfinder.scaleType)
        set(value) {
            cameraViewfinder.scaleType = getScaleType(value, position)
        }

    /**
     * The [Listener] to listen to specific view events.
     */
    var listener: Listener? = null

    private var touchUpEvent: MotionEvent? = null

    private val pinchGesture = ScaleGestureDetector(
        context, PinchToZoomOnScaleGestureListener()
    )

    private fun registerStreamer(streamer: IVideoStreamer) {
        val lifecycleScope = requireNotNull(lifecycleScope) { "LifecycleScope is not available" }
        sourceFlowJob = lifecycleScope.launch {
            streamer.videoSourceFlow.runningHistory()
                .collect { (previousVideoSource, newVideoSource) ->
                    var isPreviewing = false
                    if (previousVideoSource is IVideoPreviewableSource) {
                        isPreviewing = previousVideoSource.isPreviewingFlow.value
                        if (isPreviewing) {
                            Logger.i(TAG, "Stopping preview")
                            previousVideoSource.stopPreview()
                        }
                    }
                    if (isPreviewing && newVideoSource is IVideoPreviewableSource) {
                        Logger.i(TAG, "Restarting preview")
                        newVideoSource.startPreview()
                    }
                }
        }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PreviewView)

        try {
            enableZoomOnPinch = a.getBoolean(R.styleable.PreviewView_enableZoomOnPinch, true)
            enableTapToFocus = a.getBoolean(R.styleable.PreviewView_enableTapToFocus, true)

            scaleMode = ScaleMode.entryOf(
                a.getInt(
                    R.styleable.PreviewView_scaleMode, ScaleMode.FILL.value
                )
            )
            position = Position.entryOf(
                a.getInt(
                    R.styleable.PreviewView_position, Position.CENTER.value
                )
            )

        } finally {
            a.recycle()
        }

        addView(
            cameraViewfinder, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            streamer?.let {
                stopPreviewAsyncInternal(true)
                startPreviewAsyncInternal(true)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPreviewAsyncInternal(true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (streamer == null) {
            return super.onTouchEvent(event)
        }

        if (enableZoomOnPinch) {
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

        return super.onTouchEvent(event)
    }

    private fun performCameraTapOnFocus(cameraSource: ICameraSource) {
        if (enableTapToFocus) {
            // touchUpEvent == null means it's an accessibility click. Focus at the center instead.
            val x = touchUpEvent?.x ?: (width / 2f)
            val y = touchUpEvent?.y ?: (height / 2f)
            try {
                cameraSource.settings.focusMetering.onTap(
                    PointF(x, y),
                    Rect(this.x.toInt(), this.y.toInt(), width, height),
                    OrientationUtils.getSurfaceRotationDegrees(display.rotation)
                )
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to focus at $x, $y", t)
            }
        }
    }

    override fun performClick(): Boolean {
        val videoSource = streamer?.videoSourceFlow?.value
        if (videoSource is ICameraSource) {
            performCameraTapOnFocus(videoSource)
        }
        touchUpEvent = null
        return super.performClick()
    }

    /**
     * Stops the preview.
     */
    suspend fun stopPreview() {
        stopPreviewInternal()
    }

    fun stopPreviewAsync() {
        stopPreviewAsyncInternal(false)
    }

    /**
     * Starts the preview.
     */
    private fun stopPreviewAsyncInternal(shouldFailSilently: Boolean) {
        try {
            lifecycleScope?.launch {
                stopPreviewInternal()
            } ?: throw IllegalStateException("LifecycleScope is not available")
        } catch (t: Throwable) {
            if (shouldFailSilently) {
                Logger.w(TAG, t.toString(), t)
            } else {
                throw t
            }
        }
    }


    private suspend fun stopPreviewInternal() {
        streamer?.let { stopPreview(it) }
        viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
        viewfinderSurfaceRequest = null
    }

    /**
     * Starts the preview.
     */
    suspend fun startPreview() {
        startPreviewInternal(false)
    }

    /**
     * Starts the preview.
     */
    private fun startPreviewAsyncInternal(shouldFailSilently: Boolean) {
        try {
            lifecycleScope?.launch {
                startPreviewInternal(true)
            } ?: throw IllegalStateException("LifecycleScope is not available")
        } catch (t: Throwable) {
            if (shouldFailSilently) {
                Logger.w(TAG, t.toString(), t)
            } else {
                throw t
            }
        }
    }

    /**
     * Starts the preview asynchronously.
     */
    fun startPreviewAsync() {
        startPreviewAsyncInternal(false)
    }

    /**
     * Starts the preview.
     */
    private suspend fun startPreviewInternal(shouldFailSilently: Boolean) {
        try {
            if (size.width == 0 || size.height == 0) {
                return
            }

            try {
                streamer?.let {
                    setPreview(it, size)
                    startPreview(it)
                    listener?.onPreviewStarted()
                } ?: throw UnsupportedOperationException("Streamer has not been set")
            } catch (e: CancellationException) {
                Logger.w(TAG, "Preview has been cancelled")
            } catch (t: Throwable) {
                listener?.onPreviewFailed(t)
                throw t
            }
        } catch (t: Throwable) {
            if (shouldFailSilently) {
                Logger.w(TAG, t.toString(), t)
            } else {
                throw t
            }
        }
    }

    /**
     * Sets the preview if the view size is ready.
     *
     * @param streamer the camera streamer
     * @param targetViewSize the view size
     */
    private suspend fun setPreview(
        streamer: IVideoStreamer, targetViewSize: Size
    ) {
        val videoSource = streamer.videoSourceFlow.value
            ?: throw InvalidParameterException("Can't set preview: video source is null")

        Logger.d(TAG, "Target view size: $targetViewSize")

        val previewSize = getPreviewSize(videoSource, targetViewSize)

        try {
            // Request a new preview
            viewfinderSurfaceRequest = setPreview(streamer, cameraViewfinder, previewSize)
        } catch (t: Throwable) {
            viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
            viewfinderSurfaceRequest = null
            Logger.w(TAG, "Failed to get a Surface: $t", t)
            throw t
        }
    }

    private fun getPreviewSize(
        videoSource: IVideoSource,
        targetViewSize: Size
    ): Size {
        val previewSize = if (videoSource is ICameraSource) {
            /**
             * Get the closest available preview size to the view size.
             */
            getPreviewOutputSize(
                context.getCameraCharacteristics(videoSource.cameraId),
                targetViewSize,
                SurfaceHolder::class.java
            )
        } else {
            targetViewSize
        }

        Logger.d(TAG, "Selected preview size: $previewSize")

        return previewSize
    }

    companion object {
        private const val TAG = "PreviewView"

        private suspend fun setPreview(
            streamer: IVideoStreamer, viewfinder: CameraViewfinder, previewSize: Size
        ): ViewfinderSurfaceRequest {
            val videoSource = streamer.videoSourceFlow.value
            if (videoSource is IVideoPreviewableSource) {
                return videoSource.setPreview(viewfinder, previewSize)
            } else {
                if (videoSource == null) {
                    throw InvalidParameterException("Can't set preview: video source is null")
                } else {
                    throw InvalidParameterException("Can't set preview: video source is not previewable: ${videoSource::class.java.simpleName}")
                }
            }
        }

        private suspend fun startPreview(streamer: IVideoStreamer) {
            val videoSource = streamer.videoSourceFlow.value
            if (videoSource is IVideoPreviewableSource) {
                videoSource.startPreview()
            } else {
                if (videoSource == null) {
                    throw InvalidParameterException("Can't start preview: video source is null")
                } else {
                    throw InvalidParameterException("Can't start preview: video source is not previewable: ${videoSource::class.java.simpleName}")
                }
            }
        }

        private suspend fun stopPreview(streamer: IVideoStreamer) {
            val videoSource = streamer.videoSourceFlow.value
            if (videoSource is IVideoPreviewableSource) {
                videoSource.stopPreview()
            } else {
                if (videoSource == null) {
                    throw InvalidParameterException("Can't stop preview: video source is null")
                } else {
                    throw InvalidParameterException("Can't stop preview: video source is not previewable: ${videoSource::class.java.simpleName}")
                }
            }
        }

        private fun getPosition(scaleType: ScaleType): Position {
            return when (scaleType) {
                ScaleType.FILL_START -> Position.START
                ScaleType.FILL_CENTER -> Position.CENTER
                ScaleType.FILL_END -> Position.END
                ScaleType.FIT_START -> Position.START
                ScaleType.FIT_CENTER -> Position.CENTER
                ScaleType.FIT_END -> Position.END
            }
        }

        private fun getScaleMode(scaleType: ScaleType): ScaleMode {
            return when (scaleType) {
                ScaleType.FILL_START -> ScaleMode.FILL
                ScaleType.FILL_CENTER -> ScaleMode.FILL
                ScaleType.FILL_END -> ScaleMode.FILL
                ScaleType.FIT_START -> ScaleMode.FIT
                ScaleType.FIT_CENTER -> ScaleMode.FIT
                ScaleType.FIT_END -> ScaleMode.FIT
            }
        }

        private fun getScaleType(
            scaleMode: ScaleMode, position: Position
        ): ScaleType {
            when (position) {
                Position.START -> {
                    return when (scaleMode) {
                        ScaleMode.FILL -> ScaleType.FILL_START
                        ScaleMode.FIT -> ScaleType.FIT_START
                    }
                }

                Position.CENTER -> {
                    return when (scaleMode) {
                        ScaleMode.FILL -> ScaleType.FILL_CENTER
                        ScaleMode.FIT -> ScaleType.FIT_CENTER
                    }
                }

                Position.END -> {
                    return when (scaleMode) {
                        ScaleMode.FILL -> ScaleType.FILL_END
                        ScaleMode.FIT -> ScaleType.FIT_END
                    }
                }
            }
        }
    }

    private inner class PinchToZoomOnScaleGestureListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val source = streamer?.videoSourceFlow?.value
            if (source is ICameraSource) {
                val zoom = source.settings.zoom
                zoom.onPinch(detector.scaleFactor)
                listener?.onZoomRationOnPinchChanged(zoom.zoomRatio)
                return true
            } else {
                return false
            }
        }
    }


    /**
     * A listener for the [PreviewView].
     */
    interface Listener {
        /**
         * Called when the preview is started.
         */
        fun onPreviewStarted() {}

        /**
         * Called when the preview failed to start.
         */
        fun onPreviewFailed(t: Throwable) {}

        /**
         * Called when the zoom ratio is changed.
         * @param zoomRatio the new zoom ratio
         */
        fun onZoomRationOnPinchChanged(zoomRatio: Float) {}
    }

    /**
     * Options for the position of the [PreviewView] within its container.
     */
    enum class Position(val value: Int) {
        /**
         * The [PreviewView] is positioned at the top of its container.
         */
        START(0),

        /**
         * The [PreviewView] is positioned in the center of its container.
         */
        CENTER(1),

        /**
         * The [PreviewView] is positioned in the bottom of its container.
         */
        END(2);

        companion object {
            /**
             * Returns the [Position] from the given id.
             */
            internal fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }

    /**
     * Options for scaling the [PreviewView] within its container.
     */
    enum class ScaleMode(val value: Int) {
        /**
         * Scale the [PreviewView], maintaining the source aspect ratio, so it fills the entire
         * parent.
         *
         * This may cause the [PreviewView] to be cropped.
         */
        FILL(0),

        /**
         * Scale the [PreviewView], maintaining the source aspect ratio, so it is entirely contained
         * within the parent. The background area not covered by the viewfinder stream will be black
         * or the background of the [PreviewView].
         *
         *
         * Both dimensions of the [PreviewView] will be equal or less than the corresponding
         * dimensions of its parent.
         */
        FIT(1);

        companion object {
            /**
             * Returns the [ScaleMode] from the given id.
             */
            internal fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }

}