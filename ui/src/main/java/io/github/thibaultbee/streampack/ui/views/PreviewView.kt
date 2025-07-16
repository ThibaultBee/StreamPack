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
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.ICameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getCameraCharacteristics
import io.github.thibaultbee.streampack.core.elements.utils.ConflatedJob
import io.github.thibaultbee.streampack.core.elements.utils.OrientationUtils
import io.github.thibaultbee.streampack.core.elements.utils.extensions.runningHistoryNotNull
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

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

    private val _isPreviewingFlow = MutableStateFlow(false)

    /**
     * Whether the preview is running in case the source has been set to a non previewable source.
     */
    private val isPreviewingFlow = _isPreviewingFlow.asStateFlow()

    /**
     * Enables zoom on pinch gesture.
     */
    var enableZoomOnPinch: Boolean

    /**
     * Enables tap to focus.
     */
    var enableTapToFocus: Boolean

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

    private val lifecycleScope: CoroutineScope?
        get() = findViewTreeLifecycleOwner()?.lifecycleScope
    private val supervisorJob = SupervisorJob()

    private var coroutineScope: CoroutineScope? = null

    private val surfaceConflatedJob = ConflatedJob()
    private val surfaceFlow = MutableStateFlow<Surface?>(null)

    private var sourceJob: Job? = null

    /**
     * Sets the [IWithVideoSource] to preview.
     * To force the preview to start, use [startPreview].
     */
    var streamer: IWithVideoSource? = null
        /**
         * Sets the [IWithVideoSource] to preview.
         *
         * If the previous streamer was previewing, it will stop the preview and start the new one.
         * If the new streamer is already previewing, it will throw an exception. Make sure to stop
         * the preview before setting a new streamer.
         *
         * @param newStreamer the [IWithVideoSource] to preview
         */
        set(newStreamer) {
            if (field == newStreamer) {
                Logger.w(TAG, "No need to set the same video streamer")
                return
            }
            val newSource = newStreamer?.videoInput?.sourceFlow?.value as? IPreviewableSource
            if (newSource != null) {
                require(!newSource.isPreviewingFlow.value) { "Cannot set streamer while it is already previewing. Stop preview before." }
            }

            val previousSource = field?.videoInput?.sourceFlow?.value as? IPreviewableSource
            val isPreviousSourcePreviewing = previousSource?.isPreviewingFlow?.value ?: false

            sourceJob?.cancel()
            sourceJob = null
            coroutineScope?.let { scope ->
                if (isPreviousSourcePreviewing) {
                    scope.launch {
                        stopPreviewInternal()
                    }
                }

                newStreamer?.let { streamer ->
                    collectSource(scope, streamer)
                }
            } ?: Logger.e(TAG, "Trying to set streamer but lifecycleScope is not available")

            field = newStreamer
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
            viewfinder, ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
            )
        )
    }

    private suspend fun setPreview(videoSource: IPreviewableSource, surface: Surface) {
        Logger.d(TAG, "Setting preview surface: $surface")
        try {
            videoSource.setPreview(surface)
        } catch (t: IllegalStateException) {
            Logger.e(TAG, "Failed to set preview surface: $t")
        }
    }

    private suspend fun startPreviewIfNeeded(
        videoSource: IPreviewableSource,
        surface: Surface
    ) {
        setPreview(videoSource, surface)
        if (isPreviewingFlow.value) {
            Logger.i(TAG, "Starting preview")
            try {
                videoSource.startPreview()
                listener?.onPreviewStarted()
            } catch (t: Throwable) {
                if (surface.isValid) {
                    listener?.onPreviewFailed(t)
                } else {
                    // Happens when set video source is called repeatedly
                    Logger.w(TAG, "Surface is not valid: $t")
                }
            }
        }
    }

    private fun collectSource(
        coroutineScope: CoroutineScope,
        streamer: IWithVideoSource
    ) {
        sourceJob = coroutineScope.launch {
            streamer.videoInput?.sourceFlow?.runningHistoryNotNull()
                ?.collect { (previousVideoSource, newVideoSource) ->
                    if (previousVideoSource == newVideoSource) {
                        Logger.w(TAG, "No change in video source")
                    } else {
                        if (previousVideoSource is IPreviewableSource) {
                            val isPreviewing = previousVideoSource.isPreviewingFlow.value
                            if (isPreviewing) {
                                Logger.d(TAG, "Stopping preview")
                                previousVideoSource.stopPreview()
                            }
                            previousVideoSource.resetPreview()
                            previousVideoSource.requestRelease()

                        }
                        if (newVideoSource is IPreviewableSource) {
                            Logger.d(TAG, "Requesting for source $newVideoSource")
                            requestSurface(size, newVideoSource)
                        }
                    }
                }
        }
    }

    private fun collectState(coroutineScope: CoroutineScope) {
        surfaceConflatedJob += coroutineScope.launch {
            surfaceFlow.filterNotNull().collect { surface ->
                Logger.i(TAG, "New surface collected")
                val previewableSource =
                    streamer?.videoInput?.sourceFlow?.value as? IPreviewableSource
                previewableSource?.let {
                    startPreviewIfNeeded(it, surface)
                }
            }
        }

        if ((sourceJob == null) || (sourceJob?.isActive == false)) {
            streamer?.let { collectSource(coroutineScope, it) }
        }
    }

    private fun cancelState() {
        surfaceConflatedJob.cancel()
        sourceJob?.cancel()
        sourceJob = null
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)

        if (visibility == GONE) {
            lifecycleScope?.launch {
                try {
                    stopPreview()
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to stop preview", t)
                }
            } ?: Logger.e(TAG, "LifecycleScope is not available")
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            requestSurface(Size(w, h))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).apply {
            collectState(this)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        lifecycleScope?.launch {
            try {
                stopPreview()
            } catch (t: Throwable) {
                Logger.e(TAG, "Failed to stop preview", t)
            }
        } ?: Logger.e(TAG, "LifecycleScope is not available")

        cancelState()
        coroutineScope?.cancel()
        coroutineScope = null
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
            try {
                cameraSource.settings.focusMetering.onTap(
                    context,
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
        val videoSource = streamer?.videoInput?.sourceFlow?.value
        if (videoSource is ICameraSource) {
            performCameraTapOnFocus(videoSource)
        }
        touchUpEvent = null
        return super.performClick()
    }

    private fun requestSurface(size: Size) {
        val lifecycleScope = requireNotNull(lifecycleScope) { "LifecycleScope is not available" }
        lifecycleScope.launch {
            requestSurface(lifecycleScope.coroutineContext, size)
        }
    }

    private fun requestSurface(size: Size, videoSource: IPreviewableSource) {
        val lifecycleScope = requireNotNull(lifecycleScope) { "LifecycleScope is not available" }
        lifecycleScope.launch {
            requestSurface(lifecycleScope.coroutineContext, size, videoSource)
        }
    }

    /**
     * Use [requestSurface] instead.
     */
    private suspend fun requestSurface(coroutineContext: CoroutineContext, size: Size) {
        val videoSource = streamer?.videoInput?.sourceFlow?.value
        if (videoSource is IPreviewableSource) {
            requestSurface(coroutineContext, size, videoSource)
        } else {
            Logger.w(TAG, "Video source is not previewable")
        }
    }

    /**
     * Use [requestSurface] instead.
     */
    private suspend fun requestSurface(
        coroutineContext: CoroutineContext,
        size: Size,
        videoSource: IPreviewableSource
    ) {
        if (size.height == 0 || size.width == 0) {
            Logger.w(TAG, "Invalid size: $size")
            return
        }
        val previewSize = getPreviewSize(videoSource, size)
        val builder = ViewfinderSurfaceRequest.Builder(previewSize)
        viewfinderSurfaceRequest = if (videoSource is ICameraSource) {
            val cameraCharacteristics =
                context.getCameraCharacteristics(videoSource.cameraId)
            builder.populateFromCharacteristics(cameraCharacteristics).build()
        } else {
            builder.build()
        }.also { request ->
            val surface = withContext(coroutineContext + supervisorJob) {
                viewfinder.requestSurface(request)
            }
            surfaceFlow.emit(surface)
        }
    }

    /**
     * Stops the preview.
     */
    suspend fun stopPreview() {
        stopPreviewInternal()
    }

    private suspend fun stopPreviewInternal() {
        _isPreviewingFlow.emit(false)
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

    /**
     * Starts the preview.
     */
    suspend fun startPreview() {
        startPreviewInternal(false)
    }

    /**
     * Starts the preview.
     */
    private suspend fun startPreviewInternal(shouldFailSilently: Boolean) {
        try {
            val streamer = requireNotNull(streamer) { "Streamer is not set" }
            _isPreviewingFlow.emit(true)
            val videoSource = streamer.videoInput?.sourceFlow?.value
            if (videoSource !is IPreviewableSource) {
                Logger.e(
                    TAG,
                    "Video source is not previewable. Preview will start when the source is previewable"
                )
                return
            }

            val surface = surfaceFlow.value ?: run {
                Logger.w(
                    TAG,
                    "Surface is not set. Preview will start when the surface is set"
                )
                return
            }

            if (surface.isValid) {
                lifecycleScope?.launch {
                    startPreviewIfNeeded(videoSource, surface)
                } ?: Logger.e(
                    TAG,
                    "LifecycleScope is not available"
                )

            } else {
                requestSurface(size)
            }
        } catch (t: Throwable) {
            if (shouldFailSilently) {
                Logger.w(TAG, t.toString(), t)
            } else {
                throw t
            }
        }
    }

    private fun getPreviewSize(
        videoSource: IPreviewableSource,
        targetViewSize: Size
    ): Size {
        val previewSize = videoSource.getPreviewSize(targetViewSize, SurfaceHolder::class.java)

        Logger.d(TAG, "Selected preview size: $previewSize for target view size: $targetViewSize")

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
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val source = streamer?.videoInput?.sourceFlow?.value
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