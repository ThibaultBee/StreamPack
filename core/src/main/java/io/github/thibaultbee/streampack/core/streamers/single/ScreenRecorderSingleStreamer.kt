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
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.streamers.IVideoStreamer

/**
 * Creates a [SingleStreamer] with the screen as video source and an audio source (by default, the microphone).
 *
 * @param context the application context
 * @param audioSourceFactory the audio source factory. By default, it is the default microphone source factory. If set to null, you will have to set it later explicitly.
 * @param endpointFactory the [IEndpointInternal.Factory] implementation. By default, it is a [DynamicEndpointFactory].
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun ScreenRecorderSingleStreamer(
    context: Context,
    audioSourceFactory: IAudioSourceInternal.Factory? = MicrophoneSourceFactory(),
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): SingleStreamer {
    val streamer = SingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
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
 * Creates a [SingleStreamer] with the screen as video source and no audio source.
 *
 * @param context the application context
 * @param endpointFactory the [IEndpointInternal.Factory] implementation
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
suspend fun ScreenRecorderVideoOnlySingleStreamer(
    context: Context,
    endpointFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
): VideoOnlySingleStreamer {
    val streamer = VideoOnlySingleStreamer(
        context = context,
        endpointFactory = endpointFactory,
        defaultRotation = defaultRotation
    )
    streamer.setVideoSource(MediaProjectionVideoSourceFactory())
    return streamer
}

/**
 * Sets activity result from [ComponentActivity.registerForActivityResult] callback.
 */
fun IVideoStreamer<*>.setActivityResult(activityResult: ActivityResult) {
    val videoSource = videoSourceFlow.value
    if (videoSource !is IMediaProjectionSource) {
        throw IllegalStateException("Video source must be a MediaProjectionVideoSource")
    }
    videoSource.activityResult = activityResult
    if (this is IWithAudioSource) {
        if (audioSourceFlow.value is IMediaProjectionSource) {
            (audioSourceFlow.value as IMediaProjectionSource).activityResult = activityResult
        }
    }
}

/**
 * Creates a screen recorder intent.
 */
fun createScreenRecorderIntent(context: Context): Intent =
    (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).run {
        createScreenCaptureIntent()
    }