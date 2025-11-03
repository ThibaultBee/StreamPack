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
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.core.elements.sources.video.AbstractPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.controllers.CameraController
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.cameras
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.getAutoFocusModes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.isFrameRateSupported
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraDispatcherProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSizes
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraSurface
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraTimestampHelper
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CameraUtils
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.utils.CaptureRequestBuilderWithTargets
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Creates a [CameraSource] from a [Context].
 */
internal fun CameraSource(
    context: Context,
    dispatcherProvider: CameraDispatcherProvider,
    cameraId: String
) =
    CameraSource(
        context,
        dispatcherProvider,
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
        cameraId
    )


/**
 * Camera source implementation.
 *
 * Based on Camera2 API.
 */
internal class CameraSource(
    private val context: Context,
    private val dispatcherProvider: CameraDispatcherProvider,
    private val manager: CameraManager,
    override val cameraId: String
) : ICameraSourceInternal, ICameraSource, AbstractPreviewableSource() {
    private val coroutineScope = CoroutineScope(dispatcherProvider.default)

    private val controller = CameraController(manager, dispatcherProvider, cameraId, {
        dynamicRangeProfile.dynamicRange
    }, {
        defaultCaptureRequest(this)
    })

    override val settings by lazy { CameraSettings(manager, controller) }

    override val timestampOffsetInNs =
        CameraTimestampHelper.getTimeOffsetInNsToMonoClock(manager, cameraId)

    override val infoProviderFlow = MutableStateFlow(CameraInfoProvider(manager, cameraId))

    // States
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _isPreviewingFlow = MutableStateFlow(false)
    override val isPreviewingFlow = _isPreviewingFlow.asStateFlow()

    private val previewMutex = Mutex()
    private val streamMutex = Mutex()

    // Configuration
    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr

    init {
        val deviceCameras = manager.cameras
        require(deviceCameras.contains(cameraId)) {
            "Camera $cameraId is not available. Available cameras: ${
                deviceCameras.joinToString(", ")
            }"
        }

        coroutineScope.launch {
            controller.isActiveFlow.collect { isAvailable ->
                if (!isAvailable) {
                    _isStreamingFlow.emit(false)
                    _isPreviewingFlow.emit(false)
                }
            }
        }
    }

    private fun defaultCaptureRequest(
        captureRequest: CaptureRequestBuilderWithTargets
    ) {
        val fpsRange = CameraUtils.getClosestFpsRange(manager, cameraId, fps)
        captureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        if (manager.getAutoFocusModes(cameraId)
                .contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        ) {
            captureRequest.set(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        }
    }

    override suspend fun hasPreview() = controller.hasOutput(PREVIEW_NAME)

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun setPreview(surface: Surface) {
        if (isPreviewingFlow.value) {
            Logger.w(TAG, "Trying to set preview while previewing")
        }
        controller.addOutput(CameraSurface(PREVIEW_NAME, surface))
    }

    @SuppressLint("MissingPermission")
    override suspend fun resetPreviewImpl() {
        controller.removeOutput(PREVIEW_NAME)
    }

    override suspend fun getOutput() = controller.getOutput(STREAM_NAME)?.surface

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun setOutput(surface: Surface) {
        if (isStreamingFlow.value) {
            Logger.w(TAG, "Trying to set output while streaming")
        }
        controller.addOutput(CameraSurface(STREAM_NAME, surface))
    }

    @SuppressLint("MissingPermission")
    override suspend fun resetOutputImpl() {
        executeIfCameraPermission {
            controller.removeOutput(STREAM_NAME)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun configure(config: VideoSourceConfig) {
        if (!manager.isFrameRateSupported(cameraId, config.fps)) {
            Logger.w(TAG, "Camera $cameraId does not support ${config.fps} fps")
        }

        var needRestart = false
        if ((dynamicRangeProfile != config.dynamicRangeProfile)) {
            needRestart = true
        } else if (fps != config.fps) {
            if (controller.isActiveFlow.value) {
                val fpsRange = CameraUtils.getClosestFpsRange(manager, cameraId, config.fps)
                try {
                    controller.setSetting(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to set fps range: $fpsRange", e)
                }
            }
        }

        fps = config.fps
        dynamicRangeProfile = config.dynamicRangeProfile

        if (needRestart) {
            if (controller.isActiveFlow.value) {
                Logger.d(TAG, "Restarting camera session to apply new configuration")
                try {
                    controller.restartSession()
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to restart camera session", e)
                }
            } else {
                Logger.d(TAG, "Camera is not active, no need to restart session")
            }
        }
    }


    override fun <T> getPreviewSize(targetSize: Size, targetClass: Class<T>): Size {
        return CameraSizes.getPreviewOutputSize(
            manager.getCameraCharacteristics(cameraId),
            targetSize,
            targetClass
        )
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun startPreviewUnsafe() {
        if (isPreviewingFlow.value) {
            Logger.w(TAG, "Camera is already previewing")
            return
        }
        controller.addTarget(PREVIEW_NAME)
        _isPreviewingFlow.emit(true)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun startPreview(): Unit = previewMutex.withLock {
        startPreviewUnsafe()
    }

    /**
     * Starts video preview on [previewSurface].
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun startPreview(previewSurface: Surface) = previewMutex.withLock {
        if (isPreviewingFlow.value) {
            Logger.w(TAG, "Camera is already previewing")
            return
        }
        setPreview(previewSurface)
        startPreviewUnsafe()
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopPreview() = previewMutex.withLock {
        Logger.d(TAG, "Stopping preview")
        if (!isPreviewingFlow.value) {
            Logger.w(TAG, "Camera is not previewing")
            return
        }

        try {
            executeIfCameraPermission {
                controller.removeTarget(PREVIEW_NAME)
            }
        } catch (t: Throwable) {
            Logger.w(TAG, "Failed to stop preview: $t")
        } finally {
            _isPreviewingFlow.emit(false)
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun startStream() = streamMutex.withLock {
        if (isStreamingFlow.value) {
            Logger.w(TAG, "Camera is already streaming")
            return
        }
        Logger.d(TAG, "startStream")
        controller.addTarget(STREAM_NAME)
        _isStreamingFlow.emit(true)
        controller.muteVibrationAndSound()
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopStream() = streamMutex.withLock {
        Logger.d(TAG, "stopStream")
        if (!isStreamingFlow.value) {
            Logger.w(TAG, "Camera is not streaming")
            return
        }

        try {
            controller.unmuteVibrationAndSound()
            executeIfCameraPermission {
                controller.removeTarget(STREAM_NAME)
            }
        } catch (t: Throwable) {
            Logger.w(TAG, "Failed to stop stream: $t")
        } finally {
            _isStreamingFlow.emit(false)
        }
    }

    override fun release() {
        controller.unmuteVibrationAndSound()
        controller.release()
    }

    private suspend fun executeIfCameraPermission(block: suspend () -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            block()
        }
    }

    override fun toString(): String {
        return "CameraSource(id=$cameraId)"
    }

    companion object {
        private const val TAG = "CameraSource"

        private const val PREVIEW_NAME = "preview"
        private const val STREAM_NAME = "stream"
    }
}
