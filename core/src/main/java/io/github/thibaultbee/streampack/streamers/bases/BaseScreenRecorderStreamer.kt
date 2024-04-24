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
package io.github.thibaultbee.streampack.streamers.bases

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.sources.AudioSource
import io.github.thibaultbee.streampack.internal.sources.screen.ScreenSource
import io.github.thibaultbee.streampack.listeners.OnErrorListener

/**
 * A [BaseStreamer] that sends microphone and screen frames.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to capture audio
 * @param internalEndpoint the [IEndpoint] implementation
 * @param initialOnErrorListener initialize [OnErrorListener]
 */
open class BaseScreenRecorderStreamer(
    context: Context,
    enableAudio: Boolean = true,
    internalEndpoint: IEndpoint,
    initialOnErrorListener: OnErrorListener? = null
) : BaseStreamer(
    context = context,
    internalVideoSource = ScreenSource(context),
    internalAudioSource = if (enableAudio) AudioSource() else null,
    internalEndpoint = internalEndpoint,
    initialOnErrorListener = initialOnErrorListener
) {
    private val screenSource =
        (internalVideoSource as ScreenSource).apply { onErrorListener = onInternalErrorListener }

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

    /**
     * Same as [BaseStreamer] but it prepares [ScreenSource.encoderSurface].
     * You must have set [activityResult] before.
     */
    override suspend fun startStream() {
        screenSource.encoderSurface = codecSurface?.input
        super.startStream()
    }
}