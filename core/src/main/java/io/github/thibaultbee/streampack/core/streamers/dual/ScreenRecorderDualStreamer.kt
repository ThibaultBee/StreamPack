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
package io.github.thibaultbee.streampack.core.streamers.dual

import android.content.Context
import android.view.Surface
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation

/**
 * Creates a [DualStreamer] with the screen as video source and an audio source (by default, the microphone).
 *
 * @param context the application context
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun ScreenRecorderDualStreamer(
    context: Context,
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
        defaultRotation = defaultRotation
    )
    streamer.setVideoSource(MediaProjectionVideoSourceFactory())
    if (audioSourceFactory != null) {
        streamer.setAudioSource(audioSourceFactory)
    }
    return streamer
}

/**
 * Creates a [DualStreamer] with the screen as video source and no audio source.
 *
 * @param context the application context
 * @param firstEndpointFactory the [IEndpointInternal.Factory] implementation of the first output. By default, it is a [DynamicEndpointFactory].
 * @param secondEndpointFactory the [IEndpointInternal.Factory] implementation of the second output. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun ScreenRecorderVideoOnlyDualStreamer(
    context: Context,
    firstEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    secondEndpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): VideoOnlyDualStreamer {
    val streamer = VideoOnlyDualStreamer(
        context = context,
        firstEndpointFactory = firstEndpointFactory,
        secondEndpointFactory = secondEndpointFactory,
        defaultRotation = defaultRotation
    )
    streamer.setVideoSource(MediaProjectionVideoSourceFactory())
    return streamer
}