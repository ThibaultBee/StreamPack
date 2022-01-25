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
package com.github.thibaultbee.streampack.streamers.bases

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import com.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.sources.AudioCapture
import com.github.thibaultbee.streampack.internal.sources.screen.ScreenCapture
import com.github.thibaultbee.streampack.logger.ILogger

/**
 * Base class of screen recorder streamer.
 * Use this class, only if you want to implement a custom endpoint with a screen record as a source.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param endpoint a [IEndpoint] implementation
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio
 */
open class BaseScreenRecorderStreamer(
    context: Context,
    tsServiceInfo: ServiceInfo,
    endpoint: IEndpoint,
    logger: ILogger,
    enableAudio: Boolean
) : BaseStreamer(
    context = context,
    tsServiceInfo = tsServiceInfo,
    videoCapture = ScreenCapture(context, logger = logger),
    audioCapture = if (enableAudio) AudioCapture(logger) else null,
    endpoint = endpoint,
    logger = logger
) {
    private val screenCapture =
        (videoCapture as ScreenCapture).apply { onErrorListener = onInternalErrorListener }

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
        get() = screenCapture.activityResult
        /**
         * Set activity result. Must be call before [startStream].
         *
         * @param value activity result returns from [ComponentActivity.registerForActivityResult] callback.
         */
        set(value) {
            screenCapture.activityResult = value
        }

    /**
     * Same as [BaseStreamer] but it prepares [ScreenCapture.encoderSurface].
     * You must have set [activityResult] before.
     */
    override fun startStream() {
        screenCapture.encoderSurface = videoEncoder?.inputSurface
        super.startStream()
    }
}