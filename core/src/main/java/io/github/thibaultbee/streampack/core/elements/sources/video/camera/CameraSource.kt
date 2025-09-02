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
package io.github.thibaultbee.streampack.core.elements.sources.video.camera

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.AbstractPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getAutoFocusModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFrameRateSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSizes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraTimestampHelper
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Creates a [CameraSource] from a [Context].
 */
@RequiresPermission(Manifest.permission.CAMERA)
internal suspend fun CameraSource(context: Context, cameraId: String) =
    CameraSource(context.getSystemService(Context.CAMERA_SERVICE) as CameraManager, cameraId)


/**
 * Creates a [CameraSource] from a [CameraManager].
 */
@RequiresPermission(Manifest.permission.CAMERA)
internal suspend fun CameraSource(
    manager: CameraManager, cameraId: String
): CameraSource {
    require(manager.cameras.contains(cameraId)) {
        "Camera $cameraId is not available"
    }

    return CameraSource(manager, CameraController.create(manager, cameraId))
}

/**
 * Camera source implementation.
 *
 * Based on Camera2 API.
 */
internal class CameraSource(
    private val manager: CameraManager, private val controller: CameraController
) : ICameraSourceInternal, ICameraSource, AbstractPreviewableSource() {
    override val settings by lazy { CameraSettings(manager, controller) }

    override val cameraId = controller.cameraId
    override val timestampOffsetInNs =
        CameraTimestampHelper.getTimeOffsetInNsToMonoClock(manager, cameraId)

    // Surfaces that are running or will be running
    private var outputSurface: Surface? = null
    private var previewSurface: Surface? = null

    override val infoProviderFlow = MutableStateFlow(CameraInfoProvider(manager, cameraId))

    // States
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _isPreviewingFlow = MutableStateFlow(false)
    override val isPreviewingFlow = _isPreviewingFlow.asStateFlow()

    private val previewMutex = Mutex()
    private val surfaceMutex = Mutex()

    // Configuration
    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr

    override suspend fun hasPreview() = previewSurface != null

    override suspend fun setPreview(surface: Surface) {
        if (previewSurface == surface) {
            Logger.w(TAG, "Preview surface is already set")
            return
        }

        surfaceMutex.withLock {
            if (controller.isAvailableFlow.value) {
                addSurface(previewSurface, surface)
            }
            previewSurface = surface
        }
    }

    override suspend fun resetPreviewImpl() {
        stopPreviewInternal()
        surfaceMutex.withLock {
            previewSurface?.let { controller.removeOutput(it) }
            previewSurface = null
        }
    }

    override suspend fun getOutput() = outputSurface

    override suspend fun setOutput(surface: Surface) {
        if (outputSurface == surface) {
            Logger.w(TAG, "Output surface is already set")
            return
        }

        surfaceMutex.withLock {
            if (controller.isAvailableFlow.value) {
                addSurface(outputSurface, surface)
            }
            outputSurface = surface
        }
    }

    override suspend fun resetOutputImpl() {
        stopStream()
        surfaceMutex.withLock {
            outputSurface?.let {
                controller.removeOutput(it)
            }
            outputSurface = null
        }
    }

    private suspend fun addSurface(previousSurface: Surface?, surface: Surface) {
        if (previousSurface != null) {
            controller.replaceOutput(previousSurface, surface)
        } else {
            controller.addOutput(surface)
        }
    }

    private suspend fun addTargets(
        targets: List<Surface>
    ) {
        try {
            surfaceMutex.withLock {
                /**
                 * If the camera is already streaming, we can add the target to the current session.
                 */
                if (!controller.addTargets(targets)) {
                    // If the camera is not streaming, we need to create a new session
                    createCaptureSession(targets)
                }
            }
        } catch (e: IllegalStateException) {
            Logger.w(TAG, "Failed to start camera: $e")
        }
    }

    private suspend fun createCaptureSession(targets: List<Surface>) {
        val outputs = mutableListOf<Surface>()
        previewSurface?.let { outputs.add(it) }
        outputSurface?.let { outputs.add(it) }

        controller.createSessionController(
            outputs, dynamicRangeProfile.dynamicRange
        )
        startDefaultCaptureRequest(targets)
    }

    private suspend fun startDefaultCaptureRequest(
        targets: List<Surface>
    ) {
        targets.forEach { controller.addTarget(it) }

        val fpsRange = CameraUtils.getClosestFpsRange(manager, cameraId, fps)
        controller.setSetting(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        if (manager.getAutoFocusModes(cameraId)
                .contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        ) {
            controller.setSetting(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        }

        controller.setRepeatingSession()
    }

    override suspend fun configure(config: VideoSourceConfig) {
        val fpsAsInt = config.fps.toInt()
        require(fpsAsInt.toFloat() == config.fps) {
            "CameraSource only supports integer fps but got ${config.fps}"
        }
        if (!manager.isFrameRateSupported(cameraId, fpsAsInt)) {
            Logger.w(TAG, "Camera $cameraId does not support ${config.fps} fps")
        }

        var needRestart = false
        if ((dynamicRangeProfile != config.dynamicRangeProfile)) {
            needRestart = true
        } else if (fps != fpsAsInt) {
            if (controller.isAvailableFlow.value) {
                val fpsRange = CameraUtils.getClosestFpsRange(manager, cameraId, fpsAsInt)
                controller.setSetting(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            }
        }
        if (needRestart) {
            if (controller.isAvailableFlow.value) {
                controller.setDynamicRange(config.dynamicRangeProfile.dynamicRange)
            }
        }

        fps = fpsAsInt
        dynamicRangeProfile = config.dynamicRangeProfile
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun startPreviewInternal() {
        if (isPreviewingFlow.value) {
            Logger.w(TAG, "Camera is already previewing")
            return
        }

        val previewSurface = requireNotNull(previewSurface) {
            "Preview surface is not set"
        }

        addTargets(listOf(previewSurface))
        _isPreviewingFlow.emit(true)
    }


    override suspend fun startPreview() = previewMutex.withLock {
        startPreviewInternal()
    }

    private suspend fun stopPreviewInternal() {
        if (!isPreviewingFlow.value) {
            Logger.w(TAG, "Camera is not previewing")
            return
        }
        _isPreviewingFlow.emit(false)

        val previewSurface = requireNotNull(previewSurface) {
            "Preview surface is not set"
        }
        try {
            controller.removeTarget(previewSurface)
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to stop preview: $e")
        }
    }

    override suspend fun stopPreview() {
        previewMutex.withLock {
            stopPreviewInternal()
        }
    }

    override fun <T> getPreviewSize(targetSize: Size, targetClass: Class<T>): Size {
        return CameraSizes.getPreviewOutputSize(
            manager.getCameraCharacteristics(cameraId),
            targetSize,
            targetClass
        )
    }

    /**
     * Starts video preview on [previewSurface].
     */
    override suspend fun startPreview(previewSurface: Surface) = previewMutex.withLock {
        setPreview(previewSurface)
        startPreviewInternal()
    }

    override suspend fun startStream() {
        if (isStreamingFlow.value) {
            Logger.w(TAG, "Camera is already streaming")
            return
        }

        val outputSurface = requireNotNull(outputSurface) {
            "Output surface is not set"
        }

        addTargets(listOf(outputSurface))
        _isStreamingFlow.emit(true)
        controller.muteVibrationAndSound()
    }

    override suspend fun stopStream() {
        if (!isStreamingFlow.value) {
            Logger.w(TAG, "Camera is not streaming")
            return
        }

        _isStreamingFlow.emit(false)

        val outputSurface = requireNotNull(outputSurface) {
            "Output surface is not set"
        }

        try {
            controller.unmuteVibrationAndSound()
            controller.removeTarget(outputSurface)
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to stop stream: $e")
        }
    }

    override fun release() {
        controller.unmuteVibrationAndSound()
        runBlocking {
            outputSurface?.let { controller.removeTarget(it) }
            previewSurface?.let { controller.removeTarget(it) }
            _isStreamingFlow.emit(false)
            _isPreviewingFlow.emit(false)
        }
        controller.release()
    }

    companion object {
        private const val TAG = "CameraSource"
    }
}
