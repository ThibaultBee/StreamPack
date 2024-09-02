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
package io.github.thibaultbee.streampack.core.internal.sources.video.camera

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.internal.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.utils.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.utils.extensions.isFrameRateSupported
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

class CameraSource(
    private val context: Context,
) : IVideoSource, IPublicCameraSource {
    var previewSurface: Surface? = null
        set(value) {
            if (field == value) {
                Log.w(TAG, "Preview surface is already set to $value")
                return
            }
            if (cameraController.isCameraRunning) {
                if (value == null) {
                    stopPreview()
                } else {
                    Log.e(TAG, "Need to restart camera to change preview surface")
                    field = value
                    runBlocking {
                        restartCamera()
                    }
                }
            } else {
                field = value
            }
        }

    override var outputSurface: Surface? = null
        set(value) {
            if (field == value) {
                Log.w(TAG, "Output surface is already set to $value")
                return
            }
            if (cameraController.isCameraRunning) {
                if (value == null) {
                    runBlocking {
                        stopStream()
                    }
                } else {
                    Log.e(TAG, "Need to restart camera to change output surface")
                    field = value
                    runBlocking {
                        restartCamera()
                    }
                }
            }
            field = value
        }

    override var cameraId: String = context.defaultCameraId
        get() = cameraController.cameraId ?: field
        @RequiresPermission(Manifest.permission.CAMERA)
        set(value) {
            if (!context.isFrameRateSupported(value, fps)) {
                throw UnsupportedOperationException("Camera $value does not support $fps fps")
            }
            if (field == value) {
                Log.w(TAG, "Camera ID is already set to $value")
                return
            }

            field = value
            orientationProvider.cameraId = value
            runBlocking {
                restartCamera()
            }
        }

    private var cameraController = CameraController(context)
    override val settings = CameraSettings(context, cameraController)

    override val timestampOffset = CameraHelper.getTimeOffsetToMonoClock(context, cameraId)
    override val hasOutputSurface = true
    override val hasFrames = false
    override val orientationProvider = CameraOrientationProvider(context, cameraId)

    override fun getFrame(buffer: ByteBuffer): Frame {
        throw UnsupportedOperationException("Camera expects to run in Surface mode")
    }

    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr

    private val isStreaming: Boolean
        get() {
            val outputSurface = outputSurface ?: return false
            return cameraController.hasTarget(outputSurface)
        }
    private val isPreviewing: Boolean
        get() {
            val previewSurface = previewSurface ?: return false
            return cameraController.hasTarget(previewSurface)
        }

    override fun configure(config: VideoConfig) {
        this.fps = config.fps
        this.dynamicRangeProfile = config.dynamicRangeProfile
    }

    private suspend fun restartCamera() {
        val surfacesToRestart = listOfNotNull(previewSurface, outputSurface).filter {
            cameraController.hasTarget(it)
        }
        cameraController.stop()
        if (surfacesToRestart.isNotEmpty()) {
            startCameraRequestSessionIfNeeded(surfacesToRestart)
        } else {
            Log.w(TAG, "Trying to restart camera without surfaces")
        }
    }

    private suspend fun startCameraRequestSessionIfNeeded(sessionTargets: List<Surface>) {
        if (!cameraController.isCameraRunning) {
            val targets = mutableListOf<Surface>()
            previewSurface?.let { targets.add(it) }
            outputSurface?.let { targets.add(it) }

            cameraController.startCamera(
                cameraId, targets, dynamicRangeProfile.dynamicRange
            )
        }

        if (!cameraController.isRequestSessionRunning) {
            try {
                cameraController.startRequestSession(fps, sessionTargets)
            } catch (t: Throwable) {
                cameraController.stop()
                throw t
            }
        } else {
            cameraController.addTargets(sessionTargets)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun startPreview() {
        if (isPreviewing) {
            Log.w(TAG, "Camera is already previewing")
            return
        }

        val previewSurface = requireNotNull(previewSurface)
        startCameraRequestSessionIfNeeded(listOf(previewSurface))
    }

    fun stopPreview() {
        if (!isPreviewing) {
            Log.w(TAG, "Camera is not previewing")
            return
        }

        cameraController.removeTarget(requireNotNull(previewSurface))
    }

    override suspend fun startStream() {
        if (isStreaming) {
            Log.w(TAG, "Camera is already streaming")
            return
        }

        val outputSurface = requireNotNull(outputSurface)
        startCameraRequestSessionIfNeeded(listOf(outputSurface))

        cameraController.muteVibrationAndSound()
    }

    override suspend fun stopStream() {
        if (!isStreaming) {
            Log.w(TAG, "Camera is not streaming")
            return
        }

        cameraController.unmuteVibrationAndSound()
        cameraController.removeTarget(requireNotNull(outputSurface))
    }

    override fun release() {
        cameraController.release()
    }

    companion object {
        private const val TAG = "CameraSource"
    }
}
