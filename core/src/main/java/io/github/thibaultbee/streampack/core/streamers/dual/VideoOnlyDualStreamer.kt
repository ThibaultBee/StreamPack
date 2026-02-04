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
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.DefaultSurfaceProcessorFactory
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal
import io.github.thibaultbee.streampack.core.elements.processing.video.ISurfaceProcessorInternal.Factory
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.setCameraId
import io.github.thibaultbee.streampack.core.pipelines.DispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.core.pipelines.inputs.IVideoInput
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.IConfigurableVideoEncodingPipelineOutput
import io.github.thibaultbee.streampack.core.streamers.single.VideoConfig


/**
 * Creates a [VideoOnlyDualStreamer] with the camera as video source and no audio source.
 *
 * @param context the application context
 * @param cameraId the camera id to use. By default, it is the default camera.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation to use to create the video processor. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
@RequiresPermission(Manifest.permission.CAMERA)
suspend fun cameraVideoOnlyDualStreamer(
    context: Context,
    cameraId: String = context.defaultCameraId,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): VideoOnlyDualStreamer {
    val streamer = VideoOnlyDualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )
    streamer.setCameraId(cameraId)
    return streamer
}

/**
 * Creates a [DualStreamer] with the screen as video source and no audio source.
 *
 * @param context the application context
 * @param mediaProjection the media projection. It can be obtained with [MediaProjectionManager.getMediaProjection]. Don't forget to call [MediaProjection.stop] when you are done.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation to use to create the video processor. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
suspend fun videoMediaProjectionVideoOnlyDualStreamer(
    context: Context,
    mediaProjection: MediaProjection,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): VideoOnlyDualStreamer {
    val streamer = VideoOnlyDualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )
    streamer.setVideoSource(MediaProjectionVideoSourceFactory(mediaProjection))
    return streamer
}

/**
 * Creates a [VideoOnlyDualStreamer] with a default video source.
 *
 * @param context the application context
 * @param videoSourceFactory the video source factory. If parameter is null, no audio source are set. It can be set later with [VideoOnlySingleStreamer.setVideoSource].
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation to use to create the video processor. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
suspend fun VideoOnlyDualStreamer(
    context: Context,
    videoSourceFactory: IVideoSourceInternal.Factory,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
): VideoOnlyDualStreamer {
    val streamer = VideoOnlyDualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )
    streamer.setVideoSource(videoSourceFactory)
    return streamer
}

/**
 * A [IDualStreamer] implementation for video only (without audio).
 *
 * @param context the application context
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 * @param surfaceProcessorFactory the [ISurfaceProcessorInternal.Factory] implementation to use to create the video processor. By default, it is a [DefaultSurfaceProcessorFactory].
 * @param dispatcherProvider the [IDispatcherProvider] implementation. By default, it is a [DispatcherProvider].
 */
class VideoOnlyDualStreamer(
    context: Context,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation,
    surfaceProcessorFactory: Factory = DefaultSurfaceProcessorFactory(),
    dispatcherProvider: IDispatcherProvider = DispatcherProvider()
) : IDualStreamer, IVideoDualStreamer {
    private val streamer = DualStreamerImpl(
        context = context,
        withAudio = false,
        withVideo = true,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation,
        surfaceProcessorFactory = surfaceProcessorFactory,
        dispatcherProvider = dispatcherProvider
    )

    override val first = streamer.first as IConfigurableVideoEncodingPipelineOutput
    override val second = streamer.second as IConfigurableVideoEncodingPipelineOutput

    override val throwableFlow = streamer.throwableFlow
    override val isOpenFlow = streamer.isOpenFlow
    override val isStreamingFlow = streamer.isStreamingFlow

    override val videoInput: IVideoInput = streamer.videoInput

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) =
        streamer.setTargetRotation(rotation)

    override suspend fun setVideoConfig(videoConfig: DualStreamerVideoConfig) =
        streamer.setVideoConfig(videoConfig)

    override suspend fun close() = streamer.close()

    override suspend fun startStream() = streamer.startStream()

    override suspend fun stopStream() = streamer.stopStream()

    override suspend fun release() = streamer.release()
}

/**
 * Sets video configuration.
 *
 * It is a shortcut for [VideoOnlyDualStreamer.setVideoConfig] when both
 * outputs use the same video configuration.
 *
 * @param videoConfig the video configuration to set
 */
suspend fun VideoOnlyDualStreamer.setVideoConfig(videoConfig: VideoConfig) {
    setVideoConfig(DualStreamerVideoConfig(videoConfig))
}

