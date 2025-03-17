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
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.combineStates
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.CameraStreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.infos.StreamerConfigurationInfo
import io.github.thibaultbee.streampack.core.streamers.interfaces.ICoroutineStreamer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * A class that handles one audio and video output.
 *
 * @param context the application context
 * @param hasAudio [Boolean.true] to capture audio. It can't be changed after instantiation.
 * @param hasVideo [Boolean.true] to capture video. It can't be changed after instantiation.
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class SingleStreamer(
    protected val context: Context,
    val hasAudio: Boolean = true,
    val hasVideo: Boolean = true,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : ICoroutineSingleStreamer, ICoroutineAudioSingleStreamer, ICoroutineVideoSingleStreamer {
    private val pipeline = StreamerPipeline(
        context,
        hasAudio,
        hasVideo
    )
    private val pipelineOutput: IEncodingPipelineOutputInternal = runBlocking {
        pipeline.addOutput(
            endpointInternalFactory,
            defaultRotation
        ) as IEncodingPipelineOutputInternal
    }

    override val throwableFlow: StateFlow<Throwable?> =
        combineStates(pipeline.throwableFlow, pipelineOutput.throwableFlow) { throwableArray ->
            throwableArray[0] ?: throwableArray[1]
        }

    override val isOpenFlow: StateFlow<Boolean>
        get() = pipelineOutput.isOpenFlow

    override val isStreamingFlow: StateFlow<Boolean> = combineStates(
        pipelineOutput.isStreamingFlow,
        pipeline.isStreamingFlow
    ) { isStreamingArray ->
        isStreamingArray[0] && isStreamingArray[1]
    }

    // AUDIO
    /**
     * The audio source.
     * It allows advanced audio source settings.
     */
    override val audioSourceFlow = pipeline.audioSourceFlow
    override val audioProcessor = pipeline.audioProcessor
    override val audioEncoder: IEncoder?
        get() = pipelineOutput.audioEncoder

    override suspend fun setAudioSource(audioSource: IAudioSourceInternal) =
        pipeline.setAudioSource(audioSource)

    // VIDEO
    /**
     * The video source.
     * It allows advanced video source settings.
     */
    override val videoSourceFlow = pipeline.videoSourceFlow
    override val videoEncoder: IEncoder?
        get() = pipelineOutput.videoEncoder

    override suspend fun setVideoSource(videoSource: IVideoSourceInternal) =
        pipeline.setVideoSource(videoSource)

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun setCameraId(cameraId: String) = pipeline.setCameraId(cameraId)

    // ENDPOINT
    override val endpoint: IEndpoint
        get() = pipelineOutput.endpoint

    /**
     * The target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override var targetRotation: Int
        @RotationValue get() = pipeline.targetRotation
        set(@RotationValue newTargetRotation) {
            pipeline.targetRotation = newTargetRotation
        }

    /**
     * Gets configuration information.
     *
     * Could throw an exception if the endpoint needs to infer the configuration from the
     * [MediaDescriptor].
     * In this case, prefer using [getInfo] with the [MediaDescriptor] used in [open].
     */
    override val info: IConfigurationInfo
        get() = if (videoSourceFlow.value is CameraSource) {
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
        return if (videoSourceFlow.value is CameraSource) {
            CameraStreamerConfigurationInfo(endpointInfo)
        } else {
            StreamerConfigurationInfo(endpointInfo)
        }
    }

    // CONFIGURATION
    /**
     * Whether the streamer has audio configuration.
     */
    val hasAudioConfig: Boolean
        get() = pipelineOutput.audioCodecConfigFlow.value != null

    override val audioConfig: AudioConfig
        get() = requireNotNull(pipelineOutput.audioCodecConfigFlow.value)

    /**
     * Configures audio settings.
     * It is the first method to call after a [SingleStreamer] instantiation.
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
     * Whether the streamer has video configuration.
     */
    val hasVideoConfig: Boolean
        get() = pipelineOutput.videoCodecConfigFlow.value != null

    override val videoConfig: VideoConfig
        get() = requireNotNull(pipelineOutput.videoCodecConfigFlow.value)

    /**
     * Configures video settings.
     * It is the first method to call after a [SingleStreamer] instantiation.
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
     * Configures both video and audio settings.
     * It is the first method to call after a [SingleStreamer] instantiation.
     * It must be call when both stream and audio and video capture are not running.
     *
     * Use [IConfigurationInfo] to get value limits.
     *
     * If video encoder does not support [VideoConfig.level] or [VideoConfig.profile], it fallbacks
     * to video encoder default level and default profile.
     *
     * @param audioConfig Audio configuration to set
     * @param videoConfig Video configuration to set
     *
     * @throws [Throwable] if configuration can not be applied.
     * @see [ICoroutineStreamer.release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun setConfig(audioConfig: AudioConfig, videoConfig: VideoConfig) {
        setAudioConfig(audioConfig)
        setVideoConfig(videoConfig)
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

    companion object {
        const val TAG = "SingleStreamer"
    }
}