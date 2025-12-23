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
import android.view.Surface
import android.view.SurfaceHolder
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.CameraViewfinderExt.requestSurface
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.populateFromCharacteristics
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSettings.FocusMetering.Companion.DEFAULT_AUTO_CANCEL_DURATION_MS
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getCameraCharacteristics
import io.github.thibaultbee.streampack.core.elements.utils.ConflatedJob
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.runningHistoryNotNull
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ui.R
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


/**
 * A [FrameLayout] containing a preview for [IPreviewableSource] sources.
 *
 * [PreviewView] displays the preview of a [IPreviewableSource] source when a streamer previewable
 * video source is set.
 *
 * It handles the display, the aspect ratio and the scaling of the preview.
 *
 * In the case, you are using it, do not call [IPreviewableSource.startPreview] (or
 * [IPreviewableSource.stopPreview]) on your side. It will be handled by the [PreviewView].
 *
 * The [Manifest.permission.CAMERA] permission must be granted before using this view.
 */
class PreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val viewfinder = CameraViewfinder(context, attrs, defStyle)

    private var viewfinderSurfaceRequest: ViewfinderSurfaceRequest? = null

    /**
     * Enables zoom on pinch gesture.
     */
    var enableZoomOnPinch: Boolean

    /**
     * Enables tap to focus.
     */
    var enableTapToFocus: Boolean

    /**
     * The duration in milliseconds after which the focus area set by tap-to-focus is cleared.
     */
    var onTapToFocusTimeoutMs = DEFAULT_AUTO_CANCEL_DURATION_MS

    /**
     * The position of the [PreviewView] within its container.
     */
    var position: Position
        get() = getPosition(viewfinder.scaleType)
        set(value) {
            viewfinder.scaleType = getScaleType(scaleMode, value)
        }

    /**
     * The scale mode of the [PreviewView] within its container.
     */
    var scaleMode: ScaleMode
        get() = getScaleMode(viewfinder.scaleType)
        set(value) {
            viewfinder.scaleType = getScaleType(value, position)
        }

    /**
     * The [Listener] to listen to specific view events.
     */
    var listener: Listener? = null

    private var touchUpEvent: MotionEvent? = null

    private val pinchGesture = ScaleGestureDetector(
        context, PinchToZoomOnScaleGestureListener()
    )

    private val mainDispatcher = Dispatchers.Main
    private var lifecycleScope: CoroutineScope? = null
    private val defaultDispatcher = Dispatchers.Main
    private var defaultScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob() + CoroutineName("preview"))

    private val surfaceFlow = MutableSharedFlow<Surface?>(1)

    private var sourceJob = ConflatedJob()

    private var streamer: IWithVideoSource? = null

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
            viewfinder, ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
            )
        )

        defaultScope.launch {
            surfaceFlow.filterNotNull().collect { surface ->
                Logger.d(TAG, "New surface collected")
                val previewableSource =
                    streamer?.videoInput?.sourceFlow?.value as? IPreviewableSource?
                previewableSource?.let {
                    lifecycleScope?.launch {
                        try {
                            startPreviewIfNeeded(it, surface)
                        } catch (t: Throwable) {
                            Logger.e(TAG, "Failed to start preview: $t")
                        }
                    }
                }
            }
        }
    }

    @MainThread
    private suspend fun startPreviewIfNeeded(
        videoSource: IPreviewableSource,
        surface: Surface
    ) {
        try {
            Logger.i(TAG, "Starting preview")
            videoSource.startPreview(surface)
            listener?.onPreviewStarted()
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to start preview: $t")
            listener?.onPreviewFailed(t)
            try {
                videoSource.resetPreview()
            } catch (_: Throwable) {

            }
        }
    }

    private fun collectSource(
        streamer: IWithVideoSource
    ) {
        sourceJob += defaultScope.launch {
            streamer.videoInput?.sourceFlow?.runningHistoryNotNull()
                ?.collect { (previousVideoSource, newVideoSource) ->
                    if (previousVideoSource == newVideoSource) {
                        Logger.w(TAG, "No change in video source")
                    } else {
                        if (previousVideoSource is IPreviewableSource) {
                            previousVideoSource.stopPreview()
                            previousVideoSource.resetPreview()
                            previousVideoSource.requestRelease()

                        }
                        if (newVideoSource is IPreviewableSource) {
                            attachToStreamerIfReady(true)
                        }
                    }
                }
        }
    }

    /**
     * Sets the [IWithVideoSource] to preview.
     *
     * If the previous streamer was previewing, it will stop the preview and start the new one.
     * If the new streamer is already previewing, it will throw an exception. Make sure to stop
     * the preview before setting a new streamer.
     *
     * @param newStreamer the [IWithVideoSource] to preview
     */
    suspend fun setVideoSourceProvider(newStreamer: IWithVideoSource? = null) {
        withContext(mainDispatcher) {
            if (streamer != newStreamer) {
                /**
                 * If streamer is not the same, we stop the previous previewing one.
                 */
                val previousSource = streamer?.videoInput?.sourceFlow?.value as? IPreviewableSource
                previousSource?.stopPreview()
                previousSource?.resetPreview()
            }

            streamer = newStreamer
            if (newStreamer != null) {
                collectSource(newStreamer)
            } else {
                sourceJob.cancel()
            }
        }
    }

    @MainThread
    private fun attachToStreamerIfReady(shouldFailSilently: Boolean) {
        if (streamer != null && isAttachedToWindow) {
            try {
                requestSurface(size)
            } catch (t: Throwable) {
                if (shouldFailSilently) {
                    // Swallow the exception and fail silently if the method is invoked by View
                    // events.
                    Logger.e(TAG, "Failed to request surface $t")
                } else {
                    throw t
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Logger.d(TAG, "onSizeChanged")
        if (w != oldw || h != oldh) {
            requestSurface(Size(w, h))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Logger.d(TAG, "onAttachedToWindow")

        lifecycleScope = CoroutineScope(mainDispatcher + SupervisorJob())
        attachToStreamerIfReady(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Logger.d(TAG, "onDetachedFromWindow")

        lifecycleScope?.launch {
            try {
                stopPreview()
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to stop preview", t)
            }
        }

        lifecycleScope?.cancel()
        lifecycleScope = null
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

        return true
    }

    private fun performCameraTapOnFocus(cameraSource: ICameraSource) {
        if (enableTapToFocus) {
            // touchUpEvent == null means it's an accessibility click. Focus at the center instead.
            val x = touchUpEvent?.x ?: (width / 2f)
            val y = touchUpEvent?.y ?: (height / 2f)
            defaultScope.launch {
                try {
                    cameraSource.settings.focusMetering.onTap(
                        context,
                        PointF(x, y),
                        Rect(
                            this@PreviewView.x.toInt(),
                            this@PreviewView.y.toInt(),
                            width,
                            height
                        ),
                        OrientationUtils.getSurfaceRotationDegrees(display.rotation),
                        onTapToFocusTimeoutMs
                    )
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to focus at $x, $y", t)
                }
            }
        }
    }

    override fun performClick(): Boolean {
        val videoSource = streamer?.videoInput?.sourceFlow?.value
        if (videoSource is ICameraSource) {
            performCameraTapOnFocus(videoSource)
        }
        touchUpEvent = null
        return super.performClick()
    }

    /**
     * Requests a [Surface] for the size and the current streamer video source.
     *
     * The [Surface] is emit to the [surfaceFlow].
     */
    private fun requestSurface(size: Size) {
        Logger.d(TAG, "Requesting surface")
        val videoSource = streamer?.videoInput?.sourceFlow?.value
        if (videoSource is IPreviewableSource) {
            requestSurface(size, videoSource)
        } else {
            Logger.w(TAG, "Video source is not previewable: $videoSource")
        }
    }

    /**
     * Requests a [Surface] for the size and the [videoSource].
     *
     * The [Surface] is emit to the [surfaceFlow].
     */
    private fun requestSurface(
        size: Size,
        videoSource: IPreviewableSource
    ) {
        if (size.height == 0 || size.width == 0 || display == null) {
            Logger.w(TAG, "Invalid size: $size")
            return
        }
        val previewSize = getPreviewSize(videoSource, size)
        val builder = ViewfinderSurfaceRequest.Builder(previewSize)
        if (videoSource is ICameraSource) {
            val cameraCharacteristics =
                context.getCameraCharacteristics(videoSource.cameraId)
            builder.populateFromCharacteristics(cameraCharacteristics)
        } else {

            val rotationDegrees = OrientationUtils.getSurfaceRotationDegrees(display.rotation)
            builder.setSourceOrientation(rotationDegrees)
        }
        requestSurface(builder)
    }

    /**
     * Use [requestSurface] instead.
     */
    private fun requestSurface(
        viewfinderBuilder: ViewfinderSurfaceRequest.Builder
    ) {
        lifecycleScope?.launch {
            val viewfinderRequest = viewfinderBuilder.build().apply {
                viewfinderSurfaceRequest = this
            }
            viewfinderRequest.getSurfaceAsync()
            val surface = viewfinder.requestSurface(viewfinderRequest)
            surfaceFlow.emit(surface)
        } ?: Logger.w(TAG, "Lifecycle scope is not available")
    }

    @MainThread
    private suspend fun stopPreview() {
        streamer?.let {
            val source = it.videoInput?.sourceFlow?.value
            if (source is IPreviewableSource) {
                source.stopPreview()
                source.resetPreview()
            }
        }
        viewfinderSurfaceRequest?.markSurfaceSafeToRelease()
        viewfinderSurfaceRequest = null
    }


    private fun getPreviewSize(
        videoSource: IPreviewableSource,
        targetViewSize: Size
    ): Size {
        val previewSize = videoSource.getPreviewSize(targetViewSize, SurfaceHolder::class.java)

        Logger.d(
            TAG,
            "Selected preview size: $previewSize for target view size: $targetViewSize"
        )

        return previewSize
    }

    companion object {
        private const val TAG = "PreviewView"

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
        private val mutex = Mutex()

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val source = streamer?.videoInput?.sourceFlow?.value
            if (source is ICameraSource) {
                val scaleFactor = detector.scaleFactor
                defaultScope.launch {
                    mutex.withLock {
                        val zoom = source.settings.zoom
                        zoom.onPinch(scaleFactor)
                        listener?.onZoomRationOnPinchChanged(zoom.getZoomRatio())
                    }
                }
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
            fun entryOf(value: Int) = entries.first { it.value == value }
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
            fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }
}
