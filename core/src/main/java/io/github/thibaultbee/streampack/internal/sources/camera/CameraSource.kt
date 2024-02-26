/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.internal.sources.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.orientation.AbstractSourceOrientationProvider
import io.github.thibaultbee.streampack.internal.sources.IVideoSource
import io.github.thibaultbee.streampack.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.internal.utils.extensions.deviceOrientation
import io.github.thibaultbee.streampack.internal.utils.extensions.isDevicePortrait
import io.github.thibaultbee.streampack.internal.utils.extensions.landscapize
import io.github.thibaultbee.streampack.internal.utils.extensions.portraitize
import io.github.thibaultbee.streampack.utils.CameraSettings
import io.github.thibaultbee.streampack.utils.cameraList
import io.github.thibaultbee.streampack.utils.defaultCameraId
import io.github.thibaultbee.streampack.utils.getFacingDirection
import io.github.thibaultbee.streampack.utils.isFrameRateSupported
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class CameraSource(
    private val context: Context,
) : IVideoSource {
    var previewSurface: Surface? = null
    override var encoderSurface: Surface? = null

    var cameraId: String = context.defaultCameraId
        get() = cameraController.cameraId ?: field
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            if (!context.isFrameRateSupported(value, fps)) {
                throw UnsupportedOperationException("Camera $value does not support $fps fps")
            }
            runBlocking {
                val restartStream = isStreaming
                val restartPreview = isPreviewing
                stopPreview()
                field = value
                if (restartPreview) {
                    startPreview(value, restartStream)
                }
            }
        }
    private var cameraController = CameraController(context)
    val settings = CameraSettings(context, cameraController)

    override val timestampOffset = CameraHelper.getTimeOffsetToMonoClock(context, cameraId)
    override val hasSurface = true
    override val hasFrames = false
    override val orientationProvider = CameraOrientationProvider(context, cameraId)

    override fun getFrame(buffer: ByteBuffer): Frame {
        throw UnsupportedOperationException("Camera expects to run in Surface mode")
    }

    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr
    private var isStreaming = false
    private var isPreviewing = false

    override fun configure(config: VideoConfig) {
        this.fps = config.fps
        this.dynamicRangeProfile = config.dynamicRangeProfile
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startPreview(cameraId: String = this.cameraId, restartStream: Boolean = false) {
        var targets = mutableListOf<Surface>()
        previewSurface?.let { targets.add(it) }
        encoderSurface?.let { targets.add(it) }
        cameraController.startCamera(cameraId, targets, dynamicRangeProfile.dynamicRange)

        targets = mutableListOf()
        previewSurface?.let { targets.add(it) }
        if (restartStream) {
            encoderSurface?.let { targets.add(it) }
        }
        cameraController.startRequestSession(fps, targets)
        isPreviewing = true
        orientationProvider.cameraId = cameraId
    }

    fun stopPreview() {
        isPreviewing = false
        cameraController.stopCamera()
    }

    private fun checkStream() =
        require(encoderSurface != null) { "encoder surface must not be null" }

    override fun startStream() {
        checkStream()

        cameraController.muteVibrationAndSound()
        cameraController.addTarget(encoderSurface!!)
        isStreaming = true
    }

    override fun stopStream() {
        if (isStreaming) {
            checkStream()

            cameraController.unmuteVibrationAndSound()

            isStreaming = false
            cameraController.removeTarget(encoderSurface!!)
        }
    }

    override fun release() {
        cameraController.release()
    }


    class CameraOrientationProvider(private val context: Context, initialCameraId: String) :
        AbstractSourceOrientationProvider() {
        private val isFrontFacingMap =
            context.cameraList.associateWith { (context.getFacingDirection(it) == CameraCharacteristics.LENS_FACING_FRONT) }

        var cameraId: String = initialCameraId
            set(value) {
                if (field == value) {
                    return
                }
                val orientationChanged = mirroredVertically != isFrontFacing(value)
                field = value
                if (orientationChanged) {
                    listeners.forEach { it.onOrientationChanged() }
                }
            }

        override val orientation: Int
            get() = when (context.deviceOrientation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 270
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 90
                else -> 0
            }

        private fun isFrontFacing(cameraId: String): Boolean {
            return isFrontFacingMap[cameraId] ?: false
        }

        override val mirroredVertically: Boolean
            get() = isFrontFacing(cameraId)

        override fun getOrientedSize(size: Size): Size {
            return if (context.isDevicePortrait) {
                size.portraitize
            } else {
                size.landscapize
            }
        }

        override fun getDefaultBufferSize(size: Size): Size {
            return Size(max(size.width, size.height), min(size.width, size.height))
        }
    }
}