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

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.extensions.defaultCameraId
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.streamers.interfaces.setCameraId

/**
 * Creates a [CameraSingleStreamer] with a default audio source.
 *
 * @param context the application context
 * @param cameraId the camera id to use. By default, it is the default camera.
 * @param audioSourceInternalFactory the audio source factory. By default, it is the default microphone source factory.
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraSingleStreamer(
    context: Context,
    cameraId: String = context.defaultCameraId,
    audioSourceInternalFactory: IAudioSourceInternal.Factory = MicrophoneSourceFactory(),
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): SingleStreamer {
    val streamer = CameraSingleStreamer(
        context, true, cameraId, endpointInternalFactory, defaultRotation
    )
    streamer.setAudioSource(audioSourceInternalFactory)
    return streamer
}

/**
 * Creates a [CameraSingleStreamer].
 *
 * @param context the application context
 * @param hasAudio [Boolean.true] if the streamer will capture audio.
 * @param cameraId the camera id to use. By default, it is the default camera.
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraSingleStreamer(
    context: Context,
    hasAudio: Boolean,
    cameraId: String = context.defaultCameraId,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): SingleStreamer {
    val streamer = SingleStreamer(
        context, hasAudio, hasVideo = true, endpointInternalFactory, defaultRotation
    )
    streamer.setCameraId(cameraId)
    return streamer
}