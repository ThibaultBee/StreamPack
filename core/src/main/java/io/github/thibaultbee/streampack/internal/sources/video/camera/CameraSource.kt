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
package io.github.thibaultbee.streampack.internal.sources.video.camera

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.utils.defaultCameraId
import io.github.thibaultbee.streampack.utils.isFrameRateSupported
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

class CameraSource(
    private val context: Context,
) : IVideoSource, IPublicCameraSource {
    var previewSurface: Surface? = null
    override var encoderSurface: Surface? = null

    override var cameraId: String = context.defaultCameraId
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
    override val settings = CameraSettings(context, cameraController)

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
}

