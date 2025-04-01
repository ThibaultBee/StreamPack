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
package io.github.thibaultbee.streampack.core.streamers.single

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.encoders.IEncoder
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.regulator.controllers.IBitrateRegulatorController
import io.github.thibaultbee.streampack.core.streamers.infos.IConfigurationInfo

/**
 * Creates a [VideoOnlySingleStreamer] with a default video source.
 *
 * @param context the application context
 * @param videoSourceFactory the video source factory. If parameter is null, no audio source are set. It can be set later with [VideoOnlySingleStreamer.setVideoSource].
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun VideoOnlySingleStreamer(
    context: Context,
    videoSourceFactory: IVideoSourceInternal.Factory,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): VideoOnlySingleStreamer {
    val streamer = VideoOnlySingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation
    )
    streamer.setVideoSource(videoSourceFactory)
    return streamer
}

/**
 * A [ISingleStreamer] implementation for video only (without audio).
 *
 * @param context the application context
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
class VideoOnlySingleStreamer(
    context: Context,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : ISingleStreamer, IVideoSingleStreamer {
    private val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        withAudio = false,
        withVideo = true,
        defaultRotation = defaultRotation
    )
    override val throwableFlow = streamer.throwableFlow
    override val isOpenFlow = streamer.isOpenFlow
    override val isStreamingFlow = streamer.isStreamingFlow

    override val endpoint: IEndpoint
        get() = streamer.endpoint
    override val info: IConfigurationInfo
        get() = streamer.info

    /**
     * Sets the target rotation.
     *
     * @param rotation the target rotation in [Surface] rotation ([Surface.ROTATION_0], ...)
     */
    override suspend fun setTargetRotation(@RotationValue rotation: Int) =
        streamer.setTargetRotation(rotation)

    override suspend fun setVideoConfig(videoConfig: VideoConfig) =
        streamer.setVideoConfig(videoConfig)

    override val videoConfigFlow = streamer.videoConfigFlow
    override val videoEncoder: IEncoder?
        get() = streamer.videoEncoder
    override val videoSourceFlow = streamer.videoSourceFlow

    override suspend fun setVideoSource(videoSourceFactory: IVideoSourceInternal.Factory) =
        streamer.setVideoSource(videoSourceFactory)

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