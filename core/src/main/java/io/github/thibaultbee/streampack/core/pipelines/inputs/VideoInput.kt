/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.core.pipelines.inputs

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.SurfaceProcessor
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.AbstractSurfaceOutput
import io.github.thibaultbee.streampack.core.elements.processing.video.source.ISourceInfoProvider
import io.github.thibaultbee.streampack.core.elements.sources.video.IPreviewableSource
import io.github.thibaultbee.streampack.core.elements.sources.video.ISurfaceSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSource
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.VideoSourceConfig
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.ConflatedJob
import io.github.thibaultbee.streampack.core.elements.utils.av.video.DynamicRangeProfile
import io.github.thibaultbee.streampack.core.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A internal class that manages a video source and a video processor.
 */
internal class VideoInput(
    private val context: Context,
    dynamicRangeProfileHint: DynamicRangeProfile = DynamicRangeProfile.sdr,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private var isStreamingJob = ConflatedJob()
    private var infoProviderJob = ConflatedJob()

    private val mutex = Mutex()

    private var surfaceProcessor: ISurfaceProcessorInternal =
        SurfaceProcessor(dynamicRangeProfileHint)

    // SOURCE
    private val videoSourceInternalFlow = MutableStateFlow<IVideoSourceInternal?>(null)

    /**
     * The video source.
     * It allows advanced video settings.
     */
    val videoSourceFlow: StateFlow<IVideoSource?> = videoSourceInternalFlow.asStateFlow()

    private val videoSource: IVideoSourceInternal?
        get() = videoSourceInternalFlow.value

    /**
     * Whether the pipeline has a video source.
     */
    val hasSource: Boolean
        get() = videoSourceInternalFlow.value != null

    private val _infoProviderFlow = MutableStateFlow<ISourceInfoProvider?>(null)
    val infoProviderFlow = _infoProviderFlow.asStateFlow()

    // CONFIG
    private val _videoSourceConfigFlow = MutableStateFlow<VideoSourceConfig?>(null)

    /**
     * The video source configuration.
     */
    val videoSourceConfigFlow = _videoSourceConfigFlow.asStateFlow()

    private val videoSourceConfig: VideoSourceConfig?
        get() = videoSourceConfigFlow.value

    val hasConfig: Boolean
        get() = videoSourceConfigFlow.value != null

    // STATE
    /**
     * Whether the video input is streaming.
     */
    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingFlow = _isStreamingFlow.asStateFlow()

    // OUTPUT
    private val outputMutex = Mutex()
    private val surfaceOutput = mutableListOf<AbstractSurfaceOutput>()

    suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory) =
        withContext(coroutineDispatcher) {
            mutex.withLock {
                val previousVideoSource = videoSourceInternalFlow.value
                val isStreaming = previousVideoSource?.isStreamingFlow?.value ?: false

                if (videoSourceFactory.isSourceEquals(previousVideoSource)) {
                    Logger.i(TAG, "Video source is already set, skipping")
                    return@withContext
                }

                if ((previousVideoSource is CameraSource) && (videoSourceFactory is CameraSourceFactory)) {
                    if (previousVideoSource.cameraId == videoSourceFactory.cameraId) {
                        Logger.i(
                            TAG,
                            "Camera id ${previousVideoSource.cameraId} is already set, skipping"
                        )
                        return@withContext
                    }

                    /**
                     * It is not possible to have 2 camera sources at the same time because of
                     * camera2 API. If the new video source is a camera source and the current one
                     * is a camera source, we release the current one ASAP.
                     */
                    previousVideoSource.stopStream()
                    previousVideoSource.release()
                }

                // Prepare new video source
                val newVideoSource = videoSourceFactory.create(context)

                videoSourceConfig?.let {
                    newVideoSource.configure(it)
                    addSourceSurface(
                        it,
                        surfaceProcessor,
                        newVideoSource
                    )
                } ?: Logger.w(
                    TAG, "Video source configuration is not set"
                )

                infoProviderJob += coroutineScope.launch {
                    newVideoSource.infoProviderFlow.collect {
                        _infoProviderFlow.emit(it)
                    }
                }

                // Start new video source
                if (isStreaming) {
                    try {
                        previousVideoSource?.stopStream()
                    } catch (t: Throwable) {
                        Logger.w(
                            TAG,
                            "setVideoSource: Can't stop previous video source: ${t.message}"
                        )
                    }

                    try {
                        newVideoSource.startStream()
                    } catch (t: Throwable) {
                        Logger.w(
                            TAG,
                            "setVideoSource: Can't start new video source: ${t.message}."
                        )
                        throw t
                    }
                }

                isStreamingJob += coroutineScope.launch {
                    newVideoSource.isStreamingFlow.collect { isStreaming ->
                        if ((!isStreaming) && isStreamingFlow.value) {
                            Logger.i(TAG, "Video source has been stopped.")
                            stopStream()
                        }
                    }
                }

                // Gets and resets output surface from previous video source.
                if (previousVideoSource is ISurfaceSourceInternal) {
                    val surface = previousVideoSource.getOutput()
                    previousVideoSource.resetOutput()
                    surface?.let { surfaceProcessor.removeInputSurface(surface) }
                }

                val isPreviewing =
                    (previousVideoSource as? IPreviewableSource)?.isPreviewingFlow?.value
                        ?: false
                /**
                 * Release previous video source only if it's not previewing.
                 * If it's previewing, it will be released when preview is stopped.
                 */
                if (!isPreviewing) {
                    try {
                        previousVideoSource?.release()
                    } catch (t: Throwable) {
                        Logger.w(
                            TAG,
                            "setVideoSource: Can't release previous video source: ${t.message}"
                        )
                    }
                }

                // Replace video source
                videoSourceInternalFlow.emit(newVideoSource)
            }
        }


    suspend fun setVideoSourceConfig(newVideoSourceConfig: VideoSourceConfig) =
        withContext(coroutineDispatcher) {
            mutex.withLock {
                if (videoSourceConfig == newVideoSourceConfig) {
                    Logger.i(TAG, "Video source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change video source configuration while streaming" }

                val previousVideoConfig = videoSourceConfig
                try {
                    applyVideoSourceConfig(previousVideoConfig, newVideoSourceConfig)
                } catch (t: Throwable) {
                    throw t
                } finally {
                    _videoSourceConfigFlow.emit(newVideoSourceConfig)
                }
            }
        }

    private suspend fun applyVideoSourceConfig(
        previousVideoConfig: VideoSourceConfig?, videoConfig: VideoSourceConfig
    ) {
        val videoSourceInternal = videoSourceInternalFlow.value
        videoSourceInternal?.configure(videoConfig)
        val outputSurface =
            if (videoSourceInternal is ISurfaceSourceInternal) videoSourceInternal.getOutput() else null

        val currentSurfaceProcessor = surfaceProcessor
        if (previousVideoConfig?.dynamicRangeProfile != videoConfig.dynamicRangeProfile) {
            releaseSurfaceProcessor()
            surfaceProcessor = buildSurfaceProcessor(videoConfig)
        } else if (previousVideoConfig.resolution != videoConfig.resolution) {
            outputSurface?.let {
                if (videoSourceInternal is ISurfaceSourceInternal) {
                    videoSourceInternal.resetOutput()
                }
                currentSurfaceProcessor.removeInputSurface(it)
                addSourceSurface(
                    videoConfig,
                    currentSurfaceProcessor,
                    videoSourceInternal
                )
            }
        }
    }

    private suspend fun addSourceSurface(
        videoSourceConfig: VideoSourceConfig,
        surfaceProcessor: ISurfaceProcessorInternal,
        videoSource: IVideoSourceInternal? = videoSourceInternalFlow.value,
    ) {
        // Adds surface processor input
        if (videoSource is ISurfaceSourceInternal) {
            videoSource.setOutput(
                surfaceProcessor.createInputSurface(
                    videoSource.infoProviderFlow.value.getSurfaceSize(
                        videoSourceConfig.resolution
                    ), videoSource.timestampOffsetInNs
                )
            )
        } else {
            Logger.w(TAG, "Video source is not a surface source")
        }
    }

    private suspend fun buildSurfaceProcessor(
        videoSourceConfig: VideoSourceConfig
    ): ISurfaceProcessorInternal {

        val newSurfaceProcessor = SurfaceProcessor(videoSourceConfig.dynamicRangeProfile)
        addSourceSurface(videoSourceConfig, newSurfaceProcessor)

        outputMutex.withLock {
            surfaceOutput.forEach { newSurfaceProcessor.addOutputSurface(it) }
        }

        return newSurfaceProcessor
    }

    suspend fun addOutputSurface(output: AbstractSurfaceOutput) {
        outputMutex.withLock {
            surfaceOutput.add(output)
            surfaceProcessor.addOutputSurface(output)
        }
    }

    suspend fun removeOutputSurface(output: Surface) {
        outputMutex.withLock {
            surfaceOutput.firstOrNull { it.surface == output }?.let {
                surfaceOutput.remove(it)
            }
            surfaceProcessor.removeOutputSurface(output)
        }
    }

    suspend fun startStream() = withContext(coroutineDispatcher) {
        mutex.withLock {
            val source =
                requireNotNull(videoSource) { "Video source must be set before starting stream" }
            if (isStreamingFlow.value) {
                Logger.w(TAG, "Stream is already running")
                return@withContext
            }
            if (!hasConfig) {
                Logger.w(TAG, "Video source config is not set")
            }
            source.startStream()
            _isStreamingFlow.emit(true)
        }
    }

    suspend fun stopStream() = withContext(coroutineDispatcher) {
        mutex.withLock {
            _isStreamingFlow.emit(false)
            try {
                videoSource?.stopStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "stopStream: Can't stop video source: ${t.message}")
            }
        }
    }

    private suspend fun releaseSurfaceProcessor() {
        val videoSource = videoSourceInternalFlow.value
        if (videoSource is ISurfaceSourceInternal) {
            videoSource.getOutput()?.let {
                surfaceProcessor.removeInputSurface(it)
            }
        }
        outputMutex.withLock {
            surfaceOutput.clear()
            surfaceProcessor.removeAllOutputSurfaces()
        }

        surfaceProcessor.release()
    }

    suspend fun release() = withContext(coroutineDispatcher) {
        mutex.withLock {
            _isStreamingFlow.emit(false)
            try {
                releaseSurfaceProcessor()
            } catch (t: Throwable) {
                Logger.w(TAG, "release: Can't release surface processor: ${t.message}")
            }
            val videoSource = videoSourceInternalFlow.value
            if (videoSource is ISurfaceSourceInternal) {
                try {
                    videoSource.resetOutput()
                } catch (t: Throwable) {
                    Logger.w(
                        TAG,
                        "release: Can't release video source output surface: ${t.message}"
                    )
                }
            }
            try {
                videoSource?.release()
            } catch (t: Throwable) {
                Logger.w(TAG, "release: Can't release video source: ${t.message}")
            }

            isStreamingJob.cancel()
            infoProviderJob.cancel()
        }
        coroutineScope.coroutineContext.cancelChildren()
    }

    companion object {
        private const val TAG = "VideoInput"
    }
}