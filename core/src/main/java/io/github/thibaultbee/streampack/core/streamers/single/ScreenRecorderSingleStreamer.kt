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
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.core.elements.endpoints.DynamicEndpointFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSource.Companion.buildDefaultMicrophoneSource
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSource
import io.github.thibaultbee.streampack.core.elements.utils.RotationValue
import io.github.thibaultbee.streampack.core.elements.utils.extensions.displayRotation

/**
 * A [SingleStreamer] that sends microphone and screen frames.
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
fun ScreenRecorderSingleStreamer(
    context: Context,
    enableMicrophone: Boolean = true,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) = ScreenRecorderSingleStreamer(
    context,
    if (enableMicrophone) buildDefaultMicrophoneSource() else null,
    endpointInternalFactory,
    defaultRotation
)

/**
 * A [SingleStreamer] that sends microphone and screen frames.
 *
 * @param context application context
 * @param audioSourceInternal the audio source implementation
 * @param endpointInternalFactory the [IEndpointInternal.Factory] implementation
 * @param defaultRotation the default rotation in [Surface] rotation ([Surface.ROTATION_0], ...). By default, it is the current device orientation.
 */
open class ScreenRecorderSingleStreamer(
    context: Context,
    audioSourceInternal: IAudioSourceInternal?,
    endpointInternalFactory: IEndpointInternal.Factory = DynamicEndpointFactory(),
    @RotationValue defaultRotation: Int = context.displayRotation
) : SingleStreamer(
    context = context,
    audioSourceInternal = audioSourceInternal,
    videoSourceInternal = MediaProjectionVideoSource(context),
    endpointInternalFactory = endpointInternalFactory,
    defaultRotation = defaultRotation
) {
    private val mediaProjectionVideoSource = (videoSourceInternal as MediaProjectionVideoSource)

    /**
     * Set/get activity result from [ComponentActivity.registerForActivityResult] callback.
     * It is mandatory to set this before [startStream].
     */
    var activityResult: ActivityResult?
        /**
         * Get activity result.
         *
         * @return activity result previously set.
         */
        get() = mediaProjectionVideoSource.activityResult
        /**
         * Set activity result. Must be call before [startStream].
         *
         * @param value activity result returns from [ComponentActivity.registerForActivityResult] callback.
         */
        set(value) {
            mediaProjectionVideoSource.activityResult = value
            if (audioSourceInternal is IMediaProjectionSource) {
                (audioSourceInternal as IMediaProjectionSource).activityResult = value
            }
        }


    companion object {
        /**
         * Create a screen record intent that must be pass to [ActivityCompat.startActivityForResult].
         * It will prompt the user whether to allow screen capture.
         *
         * @param context application/service context
         * @return the intent to pass to [ActivityCompat.startActivityForResult]
         */
        fun createScreenRecorderIntent(context: Context): Intent =
            (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).run {
                createScreenCaptureIntent()
            }
    }
}