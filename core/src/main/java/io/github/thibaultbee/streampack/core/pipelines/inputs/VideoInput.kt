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
import io.github.thibaultbee.streampack.core.elements.processing.video.outputs.ISurfaceOutput
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The public interface for the video input.
 * It provides access to the video source, the video processor, and the streaming state.
 */
interface IVideoInput {

    /**
     * Whether the video input is streaming.
     */
    val isStreamingFlow: StateFlow<Boolean>

    /**
     * Whether the pipeline has a video source.
     */
    val withSource: Boolean
        get() = sourceFlow.value != null

    /**
     * The video source
     */
    val sourceFlow: StateFlow<IVideoSource?>

    /**
     * Sets the video source.
     *
     * The previous video source will be released unless its preview is still running.
     */
    suspend fun setSource(videoSourceFactory: IVideoSourceInternal.Factory)

    /**
     * Whether the video input has a configuration.
     * It is true if the video source has been configured.
     */
    val withConfig: Boolean

    /**
     * The video processor for adding effects to the video frames.
     */
    val processor: ISurfaceProcessorInternal
}

/**
 * A internal class that manages a video source and a video processor.
 */
internal class VideoInput(
    private val context: Context,
    private val surfaceProcessorFactory: ISurfaceProcessorInternal.Factory,
    dynamicRangeProfileHint: DynamicRangeProfile = DynamicRangeProfile.sdr,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IVideoInput {
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private var isStreamingJob = ConflatedJob()
    private var infoProviderJob = ConflatedJob()

    private var isReleaseRequested = AtomicBoolean(false)

    private val videoSourceMutex = Mutex()

    override var processor: ISurfaceProcessorInternal =
        surfaceProcessorFactory.create(dynamicRangeProfileHint)
        private set

    // SOURCE
    private val sourceInternalFlow = MutableStateFlow<IVideoSourceInternal?>(null)

    /**
     * The video source.
     * It allows advanced video settings.
     */
    override val sourceFlow: StateFlow<IVideoSource?> = sourceInternalFlow.asStateFlow()

    private val source: IVideoSourceInternal?
        get() = sourceInternalFlow.value

    private val _infoProviderFlow = MutableStateFlow<ISourceInfoProvider?>(null)
    val infoProviderFlow = _infoProviderFlow.asStateFlow()

    // CONFIG
    private val _sourceConfigFlow = MutableStateFlow<VideoSourceConfig?>(null)

    /**
     * The video source configuration.
     */
    val sourceConfigFlow = _sourceConfigFlow.asStateFlow()

    private val sourceConfig: VideoSourceConfig?
        get() = sourceConfigFlow.value

    override val withConfig: Boolean
        get() = sourceConfig != null

    // STATE
    /**
     * Whether the video input is streaming.
     */
    private val _isStreamingFlow = MutableStateFlow(false)
    override val isStreamingFlow = _isStreamingFlow.asStateFlow()

    // OUTPUT
    private val outputMutex = Mutex()
    private val surfaceOutput = mutableListOf<ISurfaceOutput>()

    override suspend fun setSource(videoSourceFactory: IVideoSourceInternal.Factory) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                val previousVideoSource = sourceInternalFlow.value
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

                sourceConfig?.let {
                    newVideoSource.configure(it)
                    addSourceSurface(
                        it,
                        processor,
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
                    surface?.let { processor.removeInputSurface(surface) }
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
                sourceInternalFlow.emit(newVideoSource)
            }
        }
    }


    suspend fun setSourceConfig(newVideoSourceConfig: VideoSourceConfig) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                if (sourceConfig == newVideoSourceConfig) {
                    Logger.i(TAG, "Video source configuration is the same, skipping configuration")
                    return@withContext
                }
                require(!isStreamingFlow.value) { "Can't change video source configuration while streaming" }

                val previousVideoConfig = sourceConfig
                try {
                    applySourceConfig(previousVideoConfig, newVideoSourceConfig)
                } catch (t: Throwable) {
                    throw t
                } finally {
                    _sourceConfigFlow.emit(newVideoSourceConfig)
                }
            }
        }
    }

    private suspend fun applySourceConfig(
        previousVideoConfig: VideoSourceConfig?, videoConfig: VideoSourceConfig
    ) {
        val videoSourceInternal = sourceInternalFlow.value
        videoSourceInternal?.configure(videoConfig)
        val outputSurface =
            if (videoSourceInternal is ISurfaceSourceInternal) videoSourceInternal.getOutput() else null

        val currentSurfaceProcessor = processor
        if (previousVideoConfig?.dynamicRangeProfile != videoConfig.dynamicRangeProfile) {
            releaseSurfaceProcessor()
            processor = buildSurfaceProcessor(videoConfig)
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
        videoSource: IVideoSourceInternal? = sourceInternalFlow.value,
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
        val newSurfaceProcessor =
            surfaceProcessorFactory.create(videoSourceConfig.dynamicRangeProfile)
        addSourceSurface(videoSourceConfig, newSurfaceProcessor)

        outputMutex.withLock {
            surfaceOutput.forEach { newSurfaceProcessor.addOutputSurface(it) }
        }

        return newSurfaceProcessor
    }

    suspend fun addOutputSurface(output: ISurfaceOutput) {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }

        outputMutex.withLock {
            surfaceOutput.add(output)
            processor.addOutputSurface(output)
        }
    }

    suspend fun removeOutputSurface(output: Surface) {
        outputMutex.withLock {
            surfaceOutput.firstOrNull { it.descriptor.surface == output }?.let {
                surfaceOutput.remove(it)
            }
            processor.removeOutputSurface(output)
        }
    }

    suspend fun startStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }
        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                val source =
                    requireNotNull(source) { "Video source must be set before starting stream" }
                if (isStreamingFlow.value) {
                    Logger.w(TAG, "Stream is already running")
                    return@withContext
                }
                if (!withConfig) {
                    Logger.w(TAG, "Video source config is not set")
                }
                source.startStream()
                _isStreamingFlow.emit(true)
            }
        }
    }

    suspend fun stopStream() {
        if (isReleaseRequested.get()) {
            throw IllegalStateException("Input is released")
        }
        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                _isStreamingFlow.emit(false)
                try {
                    source?.stopStream()
                } catch (t: Throwable) {
                    Logger.w(TAG, "stopStream: Can't stop video source: ${t.message}")
                }
            }
        }
    }

    private suspend fun releaseSurfaceProcessor() {
        val videoSource = sourceInternalFlow.value
        if (videoSource is ISurfaceSourceInternal) {
            videoSource.getOutput()?.let {
                processor.removeInputSurface(it)
            }
        }
        outputMutex.withLock {
            surfaceOutput.clear()
            processor.removeAllOutputSurfaces()
        }

        processor.release()
    }

    suspend fun release() {
        if (isReleaseRequested.getAndSet(true)) {
            Logger.w(TAG, "Already released")
            return
        }

        withContext(coroutineDispatcher) {
            videoSourceMutex.withLock {
                _isStreamingFlow.emit(false)
                try {
                    releaseSurfaceProcessor()
                } catch (t: Throwable) {
                    Logger.w(TAG, "release: Can't release surface processor: ${t.message}")
                }
                val videoSource = sourceInternalFlow.value
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
    }

    companion object {
        private const val TAG = "VideoInput"
    }
}