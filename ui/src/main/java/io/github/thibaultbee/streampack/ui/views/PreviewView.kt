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
    private val defaultDispatcher = Dispatchers.Default
    private var defaultScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob() + CoroutineName("preview"))
    private var sourceJob = ConflatedJob()

    private var streamer: IWithVideoSource? = null
    private val streamerMutex = Mutex()

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
                        defaultScope.launch {
                            if (previousVideoSource is IPreviewableSource) {
                                previousVideoSource.previewMutex.withLock {
                                    previousVideoSource.stopPreview()
                                    previousVideoSource.resetPreview()
                                    previousVideoSource.requestRelease()
                                }
                            }
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
        withContext(defaultDispatcher) {
            streamerMutex.withLock {
                if (newStreamer == streamer) {
                    Logger.w(TAG, "Streamer already set")
                    return@withContext
                }

                /**
                 * If streamer is not the same, we stop the previous previewing one.
                 */
                val previousSource = streamer?.videoInput?.sourceFlow?.value as? IPreviewableSource
                previousSource?.let {
                    it.previewMutex.withLock {
                        it.stopPreview()
                        it.resetPreview()
                        it.requestRelease()
                    }
                    Logger.d(TAG, "Previous source stopped")
                }

                streamer = newStreamer
                if (newStreamer != null) {
                    collectSource(newStreamer)
                } else {
                    sourceJob.cancel()
                }
            }
        }
    }

    private fun attachToStreamerIfReady(shouldFailSilently: Boolean) {
        if (streamer != null && isAttachedToWindow) {
            try {
                startPreview(size)
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
            attachToStreamerIfReady(false)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        Logger.d(TAG, "onWindowVisibilityChanged $visibility")

        if (visibility == VISIBLE) {
            attachToStreamerIfReady(false)
        } else {
            defaultScope.launch {
                try {
                    Logger.d(TAG, "Stopping preview")
                    stopPreview()
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to stop preview", t)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Logger.d(TAG, "onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Logger.d(TAG, "onDetachedFromWindow")
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
    private fun startPreview(size: Size) {
        Logger.d(TAG, "Requesting surface for $size")
        val videoSource = streamer?.videoInput?.sourceFlow?.value
        if (videoSource is IPreviewableSource) {
            startPreview(size, videoSource)
        } else {
            Logger.w(TAG, "Video source is not previewable: $videoSource")
        }
    }

    /**
     * Requests a [Surface] for the size and the [videoSource].
     *
     * The [Surface] is emit to the [surfaceFlow].
     */
    private fun startPreview(
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
        defaultScope.launch {
            startPreview(videoSource, builder)
        }
    }

    private suspend fun startPreview(
        videoSource: IPreviewableSource, viewfinderBuilder: ViewfinderSurfaceRequest.Builder
    ) {
        try {
            Logger.d(TAG, "Starting preview")
            videoSource.previewMutex.withLock {
                /**
                 * [requestSurface] will make the current preview [Surface] invalid.
                 * Force the [videoSource] to remove the preview [Surface] before the [Surface] is invalid.
                 * It prevents race conditions with the [videoSource] configuration.
                 */
                videoSource.stopPreview()
                videoSource.resetPreview()
                val surface = requestSurface(viewfinderBuilder)
                videoSource.startPreview(surface)
            }
            Logger.d(TAG, "Preview started")
            listener?.onPreviewStarted()
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to start preview: $t")
            listener?.onPreviewFailed(t)
        }
    }

    /**
     * Use [requestSurface] instead.
     */
    private suspend fun requestSurface(
        viewfinderBuilder: ViewfinderSurfaceRequest.Builder
    ): Surface {
        return withContext(mainDispatcher) {
            val viewfinderRequest = viewfinderBuilder.build().apply {
                viewfinderSurfaceRequest = this
            }
            viewfinder.requestSurface(viewfinderRequest)
        }
    }

    private suspend fun stopPreview() {
        streamer?.let {
            val videoSource = it.videoInput?.sourceFlow?.value
            if (videoSource is IPreviewableSource) {
                Logger.d(TAG, "Stopping preview")
                videoSource.previewMutex.withLock {
                    videoSource.stopPreview()
                    videoSource.resetPreview()
                }
                Logger.d(TAG, "Preview stopped")
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
