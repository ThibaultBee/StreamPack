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
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.DefaultSurfaceProcessorFactory
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MediaProjectionAudioSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.inputs.IAudioInput
import io.github.thibaultbee.streampack.core.pipelines.inputs.IVideoInput
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo


/**
 * Creates a [SingleStreamer] with the camera as video source and an audio source (by default, the microphone).
 *
 * @param context the application context
 * @param cameraId the camera id to use. By default, it is the default camera.
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun cameraSingleStreamer(
    context: Context,
    cameraId: String = context.defaultCameraId,
    audioSourceFactory: IAudioSourceInternal.Factory? = MicrophoneSourceFactory(),
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): SingleStreamer {
    val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )
    streamer.setCameraId(cameraId)
    if (audioSourceFactory != null) {
        streamer.setAudioSource(audioSourceFactory)
    }
    return streamer
}

/**
 * Creates a [SingleStreamer] with the screen as video source and audio playback as audio source.
 *
 * @param context the application context
 * @param mediaProjection the media projection. It can be obtained with [MediaProjectionManager.getMediaProjection]. Don't forget to call [MediaProjection.stop] when you are done.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun audioVideoMediaProjectionSingleStreamer(
    context: Context,
    mediaProjection: MediaProjection,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): SingleStreamer {
    val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )

    streamer.setVideoSource(MediaProjectionVideoSourceFactory(mediaProjection))
    streamer.setAudioSource(MediaProjectionAudioSourceFactory(mediaProjection))
    return streamer
}

/**
 * Creates a [SingleStreamer] with the screen as video source and an audio source (by default, the microphone).
 *
 * @param context the application context
 * @param mediaProjection the media projection. It can be obtained with [MediaProjectionManager.getMediaProjection]. Don't forget to call [MediaProjection.stop] when you are done.
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
suspend fun videoMediaProjectionSingleStreamer(
    context: Context,
    mediaProjection: MediaProjection,
    audioSourceFactory: IAudioSourceInternal.Factory? = MicrophoneSourceFactory(),
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): SingleStreamer {
    val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )

    streamer.setVideoSource(MediaProjectionVideoSourceFactory(mediaProjection))
    if (audioSourceFactory != null) {
        streamer.setAudioSource(audioSourceFactory)
    }
    return streamer
}

/**
 * Creates a [SingleStreamer] with an audio source and a video source.
 *
 * @param context the application context
 * @param audioSourceFactory the audio source factory.
 * @param videoSourceFactory the video source factory.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
suspend fun SingleStreamer(
    context: Context,
    audioSourceFactory: IAudioSourceInternal.Factory,
    videoSourceFactory: IVideoSourceInternal.Factory,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): SingleStreamer {
    val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )
    streamer.setAudioSource(audioSourceFactory)
    streamer.setVideoSource(videoSourceFactory)
    return streamer
}

/**
 * A [ISingleStreamer] implementation for both audio and video.
 *
 * @param context the application context
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
class SingleStreamer(
    context: Context,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: ISurfaceProcessorInternal.Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider(),
) : ISingleStreamer, IAudioSingleStreamer, IVideoSingleStreamer {
    private val streamer = SingleStreamerImpl(
        context = context,
        withAudio = true,
        withVideo = true,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )

    override val throwableFlow = streamer.throwableFlow
    override val isOpenFlow = streamer.isOpenFlow
    override val isStreamingFlow = streamer.isStreamingFlow

    override val endpoint: IEndpoint
        get() = streamer.endpoint
    override val info: IConfigurationInfo
        get() = streamer.info

    override val audioConfigFlow = streamer.audioConfigFlow
    override val audioEncoder: IEncoder?
        get() = streamer.audioEncoder
    override val audioInput: IAudioInput = streamer.audioInput!!

    override val videoConfigFlow = streamer.videoConfigFlow
    override val videoEncoder: IEncoder?
        get() = streamer.videoEncoder
    override val videoInput: IVideoInput = streamer.videoInput!!

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) =
        streamer.setTargetRotation(rotation)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun setAudioConfig(audioConfig: AudioConfig) =
        streamer.setAudioConfig(audioConfig)

    override suspend fun setVideoConfig(videoConfig: VideoConfig) =
        streamer.setVideoConfig(videoConfig)

    override fun getInfo(descriptor: MediaDescriptor) = streamer.getInfo(descriptor)

    override suspend fun open(descriptor: MediaDescriptor) = streamer.open(descriptor)

    override suspend fun close() = streamer.close()

    override suspend fun startStream() = streamer.startStream()

    override suspend fun stopStream() = streamer.stopStream()

    override suspend fun release() = streamer.release()

    override fun addBitrateRegulatorController(controllerFactory: IBitrateRegulatorController.Factory) =
        streamer.addBitrateRegulatorController(controllerFactory)

    override fun removeBitrateRegulatorController() = streamer.removeBitrateRegulatorController()
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
 * @see [SingleStreamer.release]
 */
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
suspend fun SingleStreamer.setConfig(audioConfig: AudioConfig, videoConfig: VideoConfig) {
    setAudioConfig(audioConfig)
    setVideoConfig(videoConfig)
}