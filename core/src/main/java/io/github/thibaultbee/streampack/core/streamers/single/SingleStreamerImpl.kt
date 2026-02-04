/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.streamers.single

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.DefaultSurfaceProcessorFactory
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.pipelines.inputs.IAudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.IVideoInput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.StreamerConfigurationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * A [ISingleStreamer] implementation for audio and video.
 *
 * @param context the application context
 * @param withAudio `true` to capture audio. It can't be changed after instantiation.
 * @param withVideo `true` to capture video. It can't be changed after instantiation.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 */
internal class SingleStreamerImpl(
    private val context: Context,
    withAudio: Boolean,
    withVideo: Boolean,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider(),
) : ISingleStreamer, IAudioSingleStreamer, IVideoSingleStreamer {
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcherProvider.default)

    private val pipeline = StreamerPipeline(
        context,
        withAudio,
        withVideo,
        audioOutputMode = StreamerPipeline.AudioOutputMode.CALLBACK,
        surfaceProcessorFactory,
        dispatcherProvider
    )
    private val pipelineOutput: IEncodingPipelineOutputInternal =
        runBlocking(dispatcherProvider.default) {
            pipeline.createEncodingOutput(
                withAudio,
                withVideo,
                endpointFactory,
                defaultRotation
            ) as IEncodingPipelineOutputInternal
        }

    override val throwableFlow: StateFlow<Throwable?> =
        merge(pipeline.throwableFlow, pipelineOutput.throwableFlow).stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            null
        )

    override val isOpenFlow: StateFlow<Boolean>
        get() = pipelineOutput.isOpenFlow

    override val isStreamingFlow: StateFlow<Boolean> = pipeline.isStreamingFlow

    // AUDIO
    /**
     * The audio input.
     * It allows advanced audio source settings.
     */
    override val audioInput: IAudioInput
        get() = pipeline.audioInput

    override val audioEncoder: IEncoder?
        get() = pipelineOutput.audioEncoder

    override suspend fun setAudioSource(audioSourceFactory: IAudioSourceInternal.Factory) =
        pipeline.setAudioSource(audioSourceFactory)

    // VIDEO
    /**
     * The video input.
     * It allows advanced video source settings.
     */
    override val videoInput: IVideoInput
        get() = pipeline.videoInput

    override val videoEncoder: IEncoder?
        get() = pipelineOutput.videoEncoder

    // ENDPOINT
    override val endpoint: IEndpoint
        get() = pipelineOutput.endpoint

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) {
        pipeline.setTargetRotation(rotation)
    }

    /**
     * Gets configuration information.
     *
     * Could throw an exception if the endpoint needs to infer the configuration from the
     * [MediaDescriptor].
     * In this case, prefer using [getInfo] with the [MediaDescriptor] used in [open].
     */
    override val info: IConfigurationInfo
        get() = if (videoInput.sourceFlow.value is CameraSource) {
            CameraStreamerConfigurationInfo(endpoint.info)
        } else {
            StreamerConfigurationInfo(endpoint.info)
        }

    /**
     * Gets configuration information from [MediaDescriptor].
     *
     * If the endpoint is not [DynamicEndpoint], [descriptor] is unused as the endpoint type is
     * already known.
     *
     * @param descriptor the media descriptor
     */
    override fun getInfo(descriptor: MediaDescriptor): IConfigurationInfo {
        val endpointInfo = try {
            endpoint.info
        } catch (_: Throwable) {
            endpoint.getInfo(descriptor)
        }
        return if (videoInput.sourceFlow.value is CameraSource) {
            CameraStreamerConfigurationInfo(endpointInfo)
        } else {
            StreamerConfigurationInfo(endpointInfo)
        }
    }

    // CONFIGURATION
    /**
     * The audio configuration flow.
     */
    override val audioConfigFlow: StateFlow<AudioConfig?> = pipelineOutput.audioCodecConfigFlow

    /**
     * Configures audio settings.
     * It is the first method to call after a [SingleStreamerImpl] instantiation.
     * It must be call when both stream and audio capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * @param audioConfig Audio configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: AudioConfig) {
        pipelineOutput.setAudioCodecConfig(audioConfig)
    }

    /**
     * The video configuration flow.
     */
    override val videoConfigFlow: StateFlow<VideoConfig?> = pipelineOutput.videoCodecConfigFlow

    /**
     * Configures video settings.
     * It is the first method to call after a [SingleStreamerImpl] instantiation.
     * It must be call when both stream and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     */
    override suspend fun setVideoConfig(videoConfig: VideoConfig) {
        pipelineOutput.setVideoCodecConfig(videoConfig)
    }

    /**
     * Opens the streamer endpoint.
     *
     * @param descriptor Media descriptor to open
     */
    override suspend fun open(descriptor: MediaDescriptor) = pipelineOutput.open(descriptor)

    /**
     * Closes the streamer endpoint.
     */
    override suspend fun close() = pipelineOutput.close()

    /**
     * Starts audio/video stream.
     * Stream depends of the endpoint: Audio/video could be write to a file or send to a remote
     * device.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @see [stopStream]
     */
    override suspend fun startStream() = pipelineOutput.startStream()

    /**
     * Stops audio/video stream.
     *
     * Internally, it resets audio and video recorders and encoders to get them ready for another
     * [startStream] session. It explains why preview could be restarted.
     *
     * @see [startStream]
     */
    override suspend fun stopStream() = pipeline.stopStream()

    /**
     * Releases the streamer.
     */
    override suspend fun release() {
        pipeline.release()
        coroutineScope.cancel()
    }

    /**
     * Adds a bitrate regulator controller.
     *
     * Limitation: it is only available for SRT for now.
     */
    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) =
        pipelineOutput.addBitrateRegulatorController(controllerFactory)

    /**
     * Removes the bitrate regulator controller.
     */
    override fun removeBitrateRegulatorController() =
        pipelineOutput.removeBitrateRegulatorController()

    companion object Companion {
        const val TAG = "SingleStreamer"
    }
}