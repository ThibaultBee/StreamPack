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
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSource
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CameraSource(
    private val context: Context
) : ICameraSourceInternal, ICameraSource, ISurfaceSource {
    private val pendingRunningSurfaces = mutableSetOf<Surface>()

    /**
     * Mutex to avoid concurrent access to preview surface.
     */
    private val previewMutex = Mutex()

    private var _previewSurface: Surface? = null
    val previewSurface: Surface?
        get() = _previewSurface

    private suspend fun setPreviewInternal(surface: Surface) {
        if (surface == previewSurface) {
            Logger.w(TAG, "Preview surface is already set to $surface")
            return
        }

        if (cameraController.isCameraRunning()) {
            val hasRemoved = previewSurface?.let { pendingRunningSurfaces.remove(previewSurface) }
            _previewSurface = surface
            if (hasRemoved == true) {
                pendingRunningSurfaces.add(surface)
            }
            if (!cameraController.isSurfaceRegistered(surface)) {
                Logger.e(TAG, "Need to restart camera to change preview surface")
                restartCamera()
            } else if (!isPreviewing) {
                Logger.e(TAG, "Adding new preview surface")
                cameraController.addTarget(surface)
            } else {
                throw IllegalStateException("Preview surface is already set")
            }
        } else {
            // Wait for camera to be started
            _previewSurface = surface
        }
    }

    override suspend fun setPreview(surface: Surface) = previewMutex.withLock {
        setPreviewInternal(surface)
    }

    suspend fun resetPreviewSurface() {
        stopPreview()
        _previewSurface = null
    }

    private var _outputSurface: Surface? = null
    override var outputSurface: Surface?
        get() = _outputSurface
        set(value) {
            runBlocking {
                if (value != null) {
                    setOutputSurface(value)
                } else {
                    try {
                        resetOutputSurface()
                    } catch (_: Throwable) {
                        Logger.w(TAG, "Failed to reset output surface")
                    }
                }
            }
        }

    private suspend fun setOutputSurface(surface: Surface) {
        if (surface == _outputSurface) {
            Logger.w(TAG, "Preview surface is already set to $surface")
            return
        }

        if (cameraController.isCameraRunning()) {
            val hasRemoved = outputSurface?.let { pendingRunningSurfaces.remove(previewSurface) }
            _outputSurface = surface
            if (hasRemoved == true) {
                pendingRunningSurfaces.add(surface)
            }
            if (!cameraController.isSurfaceRegistered(surface)) {
                Logger.e(TAG, "Need to restart camera to change output surface")
                restartCamera()
            } else if (!isStreaming) {
                Logger.e(TAG, "Adding new output surface")
                cameraController.addTarget(surface)
            } else {
                throw IllegalStateException("Output surface is already set")
            }
        } else {
            // Wait for camera to be started
            _outputSurface = surface
        }
    }

    private suspend fun resetOutputSurface() {
        stopStream()
        _outputSurface = null
    }

    private var _cameraId: String = context.defaultCameraId
    override val cameraId: String
        get() = cameraController.cameraId ?: _cameraId

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun setCameraId(cameraId: String) {
        if (this.cameraId == cameraId) {
            Logger.w(TAG, "Camera ID is already set to $cameraId")
            return
        }
        if (!context.isFrameRateSupported(cameraId, fps)) {
            Logger.e(TAG, "Camera $cameraId does not support $fps fps")
        }

        _infoProviderFlow.emit(CameraInfoProvider(context, cameraId))
        if (cameraController.isCameraRunning()) {
            // Restart camera with new cameraId
            restartCamera(cameraId = cameraId)
        } else {
            // Wait for camera to be started
            _cameraId = cameraId
        }
    }

    private fun createRunningSurfaces(): List<Surface> {
        return mutableListOf<Surface>().apply {
            if (isPreviewing) {
                add(requireNotNull(_previewSurface))
            }
            if (isStreaming) {
                add(requireNotNull(outputSurface))
            }
        }
    }

    private val cameraController = CameraController(context)
    override val settings = CameraSettings(context, cameraController)

    override val timestampOffsetInNs = CameraHelper.getTimeOffsetInNsToMonoClock(context, cameraId)
    private val _infoProviderFlow =
        MutableStateFlow(CameraInfoProvider(context, cameraId))
    override val infoProviderFlow = _infoProviderFlow.asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    private val _isPreviewingFlow = MutableStateFlow(false)
    override val isPreviewingFlow = _isPreviewingFlow.asStateFlow()

    private var fps: Int = 30
    private var dynamicRangeProfile: DynamicRangeProfile = DynamicRangeProfile.sdr

    private val isStreaming: Boolean
        get() {
            val outputSurface = _outputSurface ?: return false
            return cameraController.isSurfaceRunning(outputSurface) || pendingRunningSurfaces.contains(
                outputSurface
            )
        }

    private val isPreviewing: Boolean
        get() {
            val previewSurface = _previewSurface ?: return false
            return cameraController.isSurfaceRunning(previewSurface) || pendingRunningSurfaces.contains(
                previewSurface
            )
        }

    override fun configure(config: VideoSourceConfig) {
        if (!context.isFrameRateSupported(cameraId, config.fps)) {
            Logger.w(TAG, "Camera $cameraId does not support ${config.fps} fps")
        }

        var needRestart = false
        if ((fps != config.fps) || (dynamicRangeProfile != config.dynamicRangeProfile)) {
            needRestart = true
        }

        fps = config.fps
        dynamicRangeProfile = config.dynamicRangeProfile

        if ((isStreaming || isPreviewing) && needRestart) {
            runBlocking {
                Logger.e(TAG, "Need to restart camera to apply new configuration")
                cameraController.stopCaptureSession()
                startCameraIfNeeded()
            }
        }
    }

    private suspend fun restartCamera(
        runningSurfaces: List<Surface> = createRunningSurfaces(),
        cameraId: String = this.cameraId
    ) {
        cameraController.stopCamera()
        startCameraIfNeeded(runningSurfaces, cameraId)
    }

    private suspend fun startCameraIfNeeded(
        runningSurfaces: List<Surface> = createRunningSurfaces(), cameraId: String = this.cameraId
    ) {
        if (!(cameraController.isCameraRunning())) {
            cameraController.startCamera(
                cameraId
            )
        }

        try {
            // If camera has been stopped
            if (!cameraController.isCameraRunning()) {
                return
            }

            if (!cameraController.isCaptureSessionRunning()) {
                val targets = mutableListOf<Surface>()
                _previewSurface?.let { targets.add(it) }
                _outputSurface?.let { targets.add(it) }

                cameraController.startCaptureSession(
                    targets,
                    dynamicRangeProfile.dynamicRange
                )
            }

            // If camera has been stopped
            if (!cameraController.isCameraRunning()) {
                return
            }

            if (runningSurfaces.isEmpty()) {
                return
            }
            if (!cameraController.isRequestSessionRunning()) {
                cameraController.startRequestSession(fps, runningSurfaces)
            } else {
                cameraController.addTargets(runningSurfaces)
            }
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to start camera: $e")
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun startPreviewInternal() {
        if (isPreviewing) {
            Logger.w(TAG, "Camera is already previewing")
            return
        }

        val previewSurface = requireNotNull(_previewSurface) {
            "Preview surface is not set"
        }
        pendingRunningSurfaces.add(previewSurface)
        startCameraIfNeeded(listOf(previewSurface), cameraId)
        _isPreviewingFlow.emit(true)
    }


    override suspend fun startPreview() = previewMutex.withLock {
        startPreviewInternal()
    }

    private suspend fun stopPreviewInternal() {
        if (!isPreviewing) {
            Logger.w(TAG, "Camera is not previewing")
            return
        }
        _isPreviewingFlow.emit(false)

        val previewSurface = requireNotNull(_previewSurface) {
            "Preview surface is not set"
        }
        try {
            pendingRunningSurfaces.remove(previewSurface)
            cameraController.removeTarget(previewSurface)
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to stop preview: $e")
        }
    }

    override suspend fun stopPreview() = previewMutex.withLock {
        stopPreviewInternal()
    }

    /**
     * Starts video preview on [previewSurface].
     */
    override suspend fun startPreview(previewSurface: Surface) = previewMutex.withLock {
        setPreviewInternal(previewSurface)
        startPreviewInternal()
    }

    override suspend fun startStream() {
        if (isStreaming) {
            Logger.w(TAG, "Camera is already streaming")
            return
        }

        val outputSurface = requireNotNull(outputSurface) {
            "Output surface is not set"
        }
        startCameraIfNeeded(listOf(outputSurface), cameraId)
        pendingRunningSurfaces.add(outputSurface)

        cameraController.muteVibrationAndSound()
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        if (!isStreaming) {
            Logger.w(TAG, "Camera is not streaming")
            return
        }

        _isStreamingFlow.emit(false)

        val outputSurface = requireNotNull(outputSurface) {
            "Output surface is not set"
        }

        try {
            pendingRunningSurfaces.remove(outputSurface)
            cameraController.unmuteVibrationAndSound()
            cameraController.removeTarget(outputSurface)
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to stop stream: $e")
        }
    }

    override fun release() {
        cameraController.release()
        runBlocking {
            _isStreamingFlow.emit(false)
            _isPreviewingFlow.emit(false)
        }
    }

    companion object {
        private const val TAG = "CameraSource"
    }
}
