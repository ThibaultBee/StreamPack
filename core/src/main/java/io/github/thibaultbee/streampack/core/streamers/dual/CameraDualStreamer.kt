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

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.elements.sources.video.camera.CameraSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation

/**
 * Creates a [CameraDualStreamer] with a default audio source.
 *
 * @param context the application context
 * @param audioSourceInternal the audio source implementation. By default, it is the default microphone source.
 * @param firstEndpointInternalFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointInternalFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraDualStreamer(
    context: Context,
    audioSourceInternal: IAudioSourceInternal = buildDefaultMicrophoneSource(),
    firstEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): DualStreamer {
    val streamer = CameraDualStreamer(
        context, true, firstEndpointInternalFactory, secondEndpointInternalFactory, defaultRotation
    )
    streamer.setAudioSource(audioSourceInternal)
    return streamer
}

/**
 * Creates a [CameraDualStreamer].
 *
 * @param context the application context
 * @param hasAudio [Boolean.true] if the streamer will capture audio.
 * @param firstEndpointInternalFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointInternalFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun CameraDualStreamer(
    context: Context,
    hasAudio: Boolean,
    firstEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): DualStreamer {
    val source = CameraSource(context)
    val streamer = DualStreamer(
        context,
        hasAudio,
        hasVideo = true,
        firstEndpointInternalFactory,
        secondEndpointInternalFactory,
        defaultRotation
    )
    streamer.setVideoSource(source)
    return streamer
}
