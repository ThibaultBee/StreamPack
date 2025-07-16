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
package io.github.thibaultbee.streampack.core.streamers.dual

import android.Manifest
import android.content.Context
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MediaProjectionAudioSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.combineStates
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.pipelines.StreamerPipeline
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableAudioVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IEncodingPipelineOutputInternal
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Creates a [DualStreamer] with a default audio source.
 *
 * @param context the application context
 * @param cameraId the camera id to use. By default, it is the default camera.
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun cameraDualStreamer(
    context: Context,
    cameraId: String = context.defaultCameraId,
    audioSourceFactory: IAudioSourceInternal.Factory? = MicrophoneSourceFactory(),
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): DualStreamer {
    val streamer = DualStreamer(
        context,
        withAudio = true,
        withVideo = true,
        firstEndpointFactory,
        secondEndpointFactory,
        defaultRotation
    )
    streamer.setCameraId(cameraId)
    if (audioSourceFactory != null) {
        streamer.audioInput!!.setSource(audioSourceFactory)
    }
    return streamer
}

/**
 * Creates a [DualStreamer] with the screen as video source and audio playback as audio source.
 *
 * @param context the application context
 * @param mediaProjection the media projection. It can be obtained with [MediaProjectionManager.getMediaProjection]. Don't forget to call [MediaProjection.stop] when you are done.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun audioVideoMediaProjectionDualStreamer(
    context: Context,
    mediaProjection: MediaProjection,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): DualStreamer {
    val streamer = DualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        withAudio = true,
        withVideo = true,
        defaultRotation = defaultRotation
    )

    streamer.videoInput!!.setSource(MediaProjectionVideoSourceFactory(mediaProjection))
    streamer.setAudioSource(MediaProjectionAudioSourceFactory(mediaProjection))
    return streamer
}

/**
 * Creates a [DualStreamer] with the screen as video source and an audio source (by default, the microphone).
 *
 * @param context the application context
 * @param mediaProjection the media projection. It can be obtained with [MediaProjectionManager.getMediaProjection]. Don't forget to call [MediaProjection.stop] when you are done.
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun videoMediaProjectionDualStreamer(
    context: Context,
    mediaProjection: MediaProjection,
    audioSourceFactory: IAudioSourceInternal.Factory? = MicrophoneSourceFactory(),
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): DualStreamer {
    val streamer = DualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        withAudio = true,
        withVideo = true,
        defaultRotation = defaultRotation
    )
    streamer.setVideoSource(MediaProjectionVideoSourceFactory(mediaProjection))
    if (audioSourceFactory != null) {
        streamer.setAudioSource(audioSourceFactory)
    }
    return streamer
}

/**
 * Creates a [DualStreamer] with an audio source and a video source.
 *
 * @param context the application context
 * @param audioSourceFactory the audio source factory.
 * @param videoSourceFactory the video source factory.
 * @param firstEndpointFactory the [IEndpointInternal] implementation of the first output. By default, it is a [DynamicEndpoint].
 * @param secondEndpointFactory the [IEndpointInternal] implementation of the second output. By default, it is a [DynamicEndpoint].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun DualStreamer(
    context: Context,
    audioSourceFactory: IAudioSourceInternal.Factory,
    videoSourceFactory: IVideoSourceInternal.Factory,
    firstEndpointFactory: IEndpointInternal.Factory,
    secondEndpointFactory: IEndpointInternal.Factory,
    @RotationValue defaultRotation: Int
): DualStreamer {
    val streamer = DualStreamer(
        context = context,
        withAudio = true,
        withVideo = true,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation
    )
    streamer.setAudioSource(audioSourceFactory)
    streamer.setVideoSource(videoSourceFactory)
    return streamer
}

/**
 * A class that handles 2 outputs.
 *
 * For example, you can use it to live stream and record simultaneously.
 *
 * @param context the application context
 * @param withAudio [Boolean.true] to capture audio. It can't be changed after instantiation.
 * @param withVideo [Boolean.true] to capture video. It can't be changed after instantiation.
 * @param firstEndpointFactory the [IEndpointInternal] implementation of the first output. By default, it is a [DynamicEndpoint].
 * @param secondEndpointFactory the [IEndpointInternal] implementation of the second output. By default, it is a [DynamicEndpoint].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class DualStreamer(
    protected val context: Context,
    val withAudio: Boolean = true,
    val withVideo: Boolean = true,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : IDualStreamer, IAudioDualStreamer, IVideoDualStreamer {
    private val pipeline = StreamerPipeline(
        context, withAudio, withVideo
    )

    private val firstPipelineOutput: IEncodingPipelineOutputInternal =
        pipeline.createEncodingOutput(
            withAudio, withVideo, firstEndpointFactory, defaultRotation
        ) as IEncodingPipelineOutputInternal

    /**
     * First output of the streamer.
     */
    override val first = firstPipelineOutput as IConfigurableAudioVideoEncodingPipelineOutput

    private val secondPipelineOutput: IEncodingPipelineOutputInternal =
        pipeline.createEncodingOutput(
            withAudio, withVideo, secondEndpointFactory, defaultRotation
        ) as IEncodingPipelineOutputInternal

    /**
     * Second output of the streamer.
     */
    override val second = secondPipelineOutput as IConfigurableAudioVideoEncodingPipelineOutput

    override val throwableFlow: StateFlow<Throwable?> = combineStates(
        pipeline.throwableFlow,
        firstPipelineOutput.throwableFlow,
        secondPipelineOutput.throwableFlow
    ) { throwableArray ->
        throwableArray[0] ?: throwableArray[1] ?: throwableArray[2]
    }

    /**
     * Whether any of the output is opening.
     */
    override val isOpenFlow: StateFlow<Boolean> = combineStates(
        firstPipelineOutput.isOpenFlow, secondPipelineOutput.isOpenFlow
    ) { isOpeningArray ->
        isOpeningArray[0] || isOpeningArray[1]
    }

    /**
     * Whether any of the output is streaming.
     */
    override val isStreamingFlow: StateFlow<Boolean> = combineStates(
        pipeline.isStreamingFlow,
        firstPipelineOutput.isStreamingFlow,
        secondPipelineOutput.isStreamingFlow
    ) { isStreamingArray ->
        isStreamingArray[0] && (isStreamingArray[1] || isStreamingArray[2])
    }

    /**
     * Closes the outputs.
     * Same as calling [first.close] and [second.close].
     */
    override suspend fun close() {
        firstPipelineOutput.close()
        secondPipelineOutput.close()
    }

    // SOURCES
    override val audioInput = pipeline.audioInput

    // PROCESSORS
    override val videoInput = pipeline.videoInput

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) {
        pipeline.setTargetRotation(rotation)
    }

    /**
     * Sets audio configuration.
     *
     * It is a shortcut for [IConfigurableAudioVideoEncodingPipelineOutput.setAudioCodecConfig].
     *
     * @param audioConfig the audio configuration to set
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: DualStreamerAudioConfig) {
        var throwable: Throwable? = null

        try {
            firstPipelineOutput.setAudioCodecConfig(audioConfig.firstAudioConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        try {
            audioConfig.secondAudioConfig.let { secondPipelineOutput.setAudioCodecConfig(it) }
        } catch (e: Throwable) {
            throwable = e
        }
        throwable?.let { throw it }
    }

    /**
     * Sets video configuration.
     *
     * It is a shortcut for [IConfigurableAudioVideoEncodingPipelineOutput.setVideoCodecConfig].
     * To only set video configuration for a specific output, use [first.setVideoCodecConfig] or
     * [second.setVideoCodecConfig] outputs.
     * In that case, you call [first.setVideoCodecConfig] or [second.setVideoCodecConfig] explicitly,
     * make sure that the frame rate for both configurations is the same.
     *
     * @param videoConfig the video configuration to set
     */
    override suspend fun setVideoConfig(videoConfig: DualStreamerVideoConfig) {
        var throwable: Throwable? = null
        try {
            firstPipelineOutput.setVideoCodecConfig(videoConfig.firstVideoConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        try {
            secondPipelineOutput.setVideoCodecConfig(videoConfig.secondVideoConfig)
        } catch (e: Throwable) {
            throwable = e
        }
        throwable?.let { throw it }
    }

    /**
     * Configures both video and audio settings.
     *
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
     * @see [DualStreamer.release]
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun setConfig(
        audioConfig: DualStreamerAudioConfig, videoConfig: DualStreamerVideoConfig
    ) {
        setAudioConfig(audioConfig)
        setVideoConfig(videoConfig)
    }


    override suspend fun startStream() = pipeline.startStream()

    override suspend fun stopStream() = pipeline.stopStream()

    override suspend fun release() = pipeline.release()
}