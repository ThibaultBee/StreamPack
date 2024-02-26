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
package io.github.thibaultbee.streampack.views

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.camera.viewfinder.CameraViewfinder.ScaleType
import androidx.camera.viewfinder.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.populateFromCharacteristics
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import io.github.thibaultbee.streampack.R
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.utils.OrientationUtils
import io.github.thibaultbee.streampack.utils.backCameraList
import io.github.thibaultbee.streampack.utils.frontCameraList
import io.github.thibaultbee.streampack.utils.getCameraCharacteristics

/**
 * A [FrameLayout] containing a preview for the [ICameraStreamer].
 *
 * It handles the display, the aspect ratio and the scaling of the preview.
 *
 * In the case, you are using it, do not call [ICameraStreamer.startPreview] or
 * [ICameraStreamer.stopPreview] on application side. It will be handled by the [PreviewView].
 *
 * The [Manifest.permission.CAMERA] permission must be granted before using this view.
 */
class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val cameraViewFinder = CameraViewfinder(context, attrs, defStyle)
    private var surfaceRequest: ViewfinderSurfaceRequest? = null

    private val cameraFacingDirection: CameraFacingDirection
    private val defaultCameraId: String?

    private var isPreviewing = false

    /**
     * Enables zoom on pinch gesture.
     */
    var enableZoomOnPinch: Boolean

    /**
     * Enables tap to focus.
     */
    var enableTapToFocus: Boolean

    /**
     * Sets the [ICameraStreamer] to preview.
     * Once set, the [PreviewView] will try to start the preview.
     *
     * Only one [ICameraStreamer] can be set.
     */
    var streamer: ICameraStreamer? = null
        /**
         * Sets the [ICameraStreamer] to preview.
         *
         * @param value the [ICameraStreamer] to preview
         */
        set(value) {
            post {
                stopPreviewInternal()
                field = value
                startPreviewInternal(size)
            }
        }

    /**
     * The position of the [PreviewView] within its container.
     */
    var position: Position
        get() = getPosition(cameraViewFinder.scaleType)
        set(value) {
            cameraViewFinder.scaleType = getScaleType(scaleMode, value)
        }

    /**
     * The scale mode of the [PreviewView] within its container.
     */
    var scaleMode: ScaleMode
        get() = getScaleMode(cameraViewFinder.scaleType)
        set(value) {
            cameraViewFinder.scaleType = getScaleType(value, position)
        }

    /**
     * The [Listener] to listen to specific view events.
     */
    var listener: Listener? = null

    private var touchUpEvent: MotionEvent? = null

    private val pinchGesture = ScaleGestureDetector(
        context,
        PinchToZoomOnScaleGestureListener()
    )

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PreviewView)

        try {
            cameraFacingDirection = CameraFacingDirection.entryOf(
                a.getInt(R.styleable.PreviewView_cameraFacingDirection, DEFAULT_CAMERA_FACING.value)
            )

            defaultCameraId = when (cameraFacingDirection) {
                CameraFacingDirection.FRONT -> {
                    context.frontCameraList.firstOrNull()
                }

                CameraFacingDirection.BACK -> {
                    context.backCameraList.firstOrNull()
                }
            }

            enableZoomOnPinch =
                a.getBoolean(R.styleable.PreviewView_enableZoomOnPinch, true)
            enableTapToFocus =
                a.getBoolean(R.styleable.PreviewView_enableTapToFocus, true)

            scaleMode = ScaleMode.entryOf(
                a.getInt(
                    R.styleable.PreviewView_scaleMode,
                    ScaleMode.FILL.value
                )
            )
            position = Position.entryOf(
                a.getInt(
                    R.styleable.PreviewView_position,
                    Position.CENTER.value
                )
            )

        } finally {
            a.recycle()
        }

        addView(
            cameraViewFinder,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        startPreviewIfReady(size, true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPreview()
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

    override fun performClick(): Boolean {
        streamer?.let {
            if (enableTapToFocus) {
                // mTouchUpEvent == null means it's an accessibility click. Focus at the center instead.
                val x = touchUpEvent?.x ?: (width / 2f)
                val y = touchUpEvent?.y ?: (height / 2f)
                it.settings.camera.focusMetering.onTap(
                    PointF(x, y),
                    Rect(this.x.toInt(), this.y.toInt(), width, height),
                    OrientationUtils.getSurfaceOrientationDegrees(display.rotation)
                )
            }

        }
        touchUpEvent = null
        return super.performClick()
    }

    /**
     * Stops the preview.
     */
    private fun stopPreview() {
        post {
            stopPreviewInternal()
        }
    }

    private fun stopPreviewInternal() {
        streamer?.stopPreview()
        surfaceRequest?.markSurfaceSafeToRelease()
        surfaceRequest = null
        isPreviewing = false
    }

    private fun startPreviewIfReady(targetViewSize: Size, shouldFailSilently: Boolean) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Camera permission is needed to run this application")
            }

            post {
                startPreviewInternal(targetViewSize)
            }
        } catch (e: Exception) {
            if (shouldFailSilently) {
                Logger.w(TAG, e.toString(), e)
            } else {
                throw e
            }
        }
    }

    private fun startPreviewInternal(
        targetViewSize: Size
    ) {
        val streamer = streamer ?: run {
            Logger.w(TAG, "Streamer has not been set")
            return
        }
        if (width == 0 || height == 0) {
            Logger.w(TAG, "View size is not ready")
            return
        }
        if (isPreviewing) {
            Logger.w(TAG, "Preview is already running")
            return
        }
        isPreviewing = true

        Logger.d(TAG, "Target view size: $targetViewSize")

        val camera = defaultCameraId ?: streamer.camera
        Logger.i(TAG, "Starting on camera: $camera")

        val request = createRequest(targetViewSize, camera)
        surfaceRequest = request

        sendRequest(request, { surface ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                surfaceRequest?.markSurfaceSafeToRelease()
                surfaceRequest = null
                isPreviewing = false
                Logger.e(
                    TAG,
                    "Camera permission is needed to run this application"
                )
            } else {
                streamer.startPreview(surface, camera)
                listener?.onPreviewStarted()
            }
        }, { t ->
            surfaceRequest?.markSurfaceSafeToRelease()
            surfaceRequest = null
            isPreviewing = false
            Logger.w(TAG, "Failed to get a Surface: $t", t)
        })
    }

    private fun createRequest(
        targetViewSize: Size,
        camera: String,
    ): ViewfinderSurfaceRequest {
        /**
         * Get the closest available preview size to the view size.
         */
        val previewSize = getPreviewOutputSize(
            context.getCameraCharacteristics(camera),
            targetViewSize,
            SurfaceHolder::class.java
        )

        Logger.d(TAG, "Selected preview size: $previewSize")

        val builder = ViewfinderSurfaceRequest.Builder(previewSize)
        builder.populateFromCharacteristics(context.getCameraCharacteristics(camera))

        return builder.build()
    }

    private fun sendRequest(
        request: ViewfinderSurfaceRequest,
        onSuccess: (Surface) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        val surfaceListenableFuture =
            cameraViewFinder.requestSurfaceAsync(request)

        Futures.addCallback(
            surfaceListenableFuture,
            object : FutureCallback<Surface> {
                override fun onSuccess(surface: Surface) {
                    onSuccess(surface)
                }

                override fun onFailure(t: Throwable) {
                    onFailure(t)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    companion object {
        private const val TAG = "PreviewView"

        private val DEFAULT_CAMERA_FACING = CameraFacingDirection.BACK


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

        private fun getScaleType(scaleMode: ScaleMode, position: Position): ScaleType {
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

    private inner class PinchToZoomOnScaleGestureListener :
        SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            streamer?.settings?.camera?.zoom?.let {
                it.onPinch(detector.scaleFactor)
                listener?.onZoomRationOnPinchChanged(it.zoomRatio)
            }
            return true
        }
    }


    interface Listener {
        fun onPreviewStarted() {}
        fun onZoomRationOnPinchChanged(zoomRatio: Float) {}
    }

    /**
     * Options for the camera facing direction.
     */
    enum class CameraFacingDirection(val value: Int) {
        /**
         * The facing of the camera is the same as that of the screen.
         */
        FRONT(0),

        /**
         * The facing of the camera is opposite to that of the screen.
         */
        BACK(1);

        companion object {
            /**
             * Returns the [CameraFacingDirection] from the given id.
             */
            internal fun entryOf(value: Int) = entries.first { it.value == value }
        }
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
