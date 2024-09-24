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
package io.github.thibaultbee.streampack.core.streamers

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.core.internal.endpoints.DynamicEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.internal.sources.audio.MicrophoneSource
import io.github.thibaultbee.streampack.core.internal.sources.video.screen.ScreenSource

/**
 * A [DefaultStreamer] that sends microphone and screen frames.
 *
 * @param context application context
 * @param enableMicrophone [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpointInternal] implementation
 */
open class DefaultScreenRecorderStreamer(
    context: Context,
    enableMicrophone: Boolean = true,
    internalEndpoint: IEndpointInternal = DynamicEndpoint(context)
) : DefaultStreamer(
    context = context,
    videoSourceInternal = ScreenSource(context),
    audioSourceInternal = if (enableMicrophone) MicrophoneSource() else null,
    endpointInternal = internalEndpoint
) {
    private val screenSource =
        (videoSourceInternal as ScreenSource).apply {
            listener = object : ScreenSource.Listener {
                override fun onStop() {
                    onStreamError(Exception("Screen source has been stopped"))
                }
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
        get() = screenSource.activityResult
        /**
         * Set activity result. Must be call before [startStream].
         *
         * @param value activity result returns from [ComponentActivity.registerForActivityResult] callback.
         */
        set(value) {
            screenSource.activityResult = value
        }
}