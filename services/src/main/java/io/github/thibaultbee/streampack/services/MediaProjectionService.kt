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
package io.github.thibaultbee.streampack.services

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.thibaultbee.streampack.core.elements.sources.IMediaProjectionSource
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MediaProjectionAudioSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.video.IVideoSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.video.mediaprojection.MediaProjectionVideoSourceFactory
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithAudioSource
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoSource
import io.github.thibaultbee.streampack.core.utils.extensions.getMediaProjection
import io.github.thibaultbee.streampack.services.utils.StreamerFactory

/**
 * Foreground bound service that manages screen recorder streamers.
 *
 * To customise this service, you have to extend this class.
 *
 * In your AndroidManifest, you have to add:
 *     <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 *     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 * ...
 *     <service
 *          android:name=".YourScreenRecorderService"
 *          android:exported="false"
 *          android:foregroundServiceType="mediaProjection" />
 *
 *  To customize notification, you can override:
 *   - R.string.service_notification_* string values
 *   - override open onNotification methods: [onOpenNotification], [onCloseNotification] and [onErrorNotification]
 *
 * @param streamerFactory the streamer factory
 * @param notificationId the notification id a unique number
 * @param channelId the notification channel id
 * @param channelNameResourceId A string resource identifier for the user visible name of the notification channel.
 * @param channelDescriptionResourceId A string resource identifier for the user visible description of the notification channel.
 * @param notificationIconResourceId A drawable resource identifier for the user visible icon of the notification channel.
 */
abstract class MediaProjectionService<T : IStreamer>(
    streamerFactory: StreamerFactory<T>,
    notificationId: Int = DEFAULT_NOTIFICATION_ID,
    channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    @StringRes channelNameResourceId: Int = R.string.default_channel_name,
    @StringRes channelDescriptionResourceId: Int = 0,
    @DrawableRes notificationIconResourceId: Int = R.drawable.ic_baseline_linked_camera_24
) : StreamerService<T>(
    streamerFactory,
    notificationId,
    channelId,
    channelNameResourceId,
    channelDescriptionResourceId,
    notificationIconResourceId
) {
    /**
     * The media projection used to stream the screen.
     *
     * It is set when the streamer is created and stopped when the service is destroyed or when the
     * streamer is stopped.
     */
    protected var mediaProjection: MediaProjection? = null
        private set

    private fun createMediaProjectionFromBundle(extras: Bundle): MediaProjection {
        val activityResultBundle =
            extras.getBundle(ACTIVITY_RESULT_KEY)
                ?: throw IllegalStateException("Activity result bundle must be pass to the service")
        val resultCode = activityResultBundle.getInt(RESULT_CODE_KEY)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityResultBundle.getParcelable(RESULT_DATA_KEY, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            activityResultBundle.getParcelable(RESULT_DATA_KEY)
        }
            ?: throw IllegalStateException("Activity result data must be pass to the service")
        return applicationContext.getMediaProjection(resultCode, resultData).also {
            this@MediaProjectionService.mediaProjection = it
        }
    }

    /**
     * Called when the streamer is stopped.
     *
     * By default, it stops the media projection but you can override it to do something else.
     */
    override fun onStreamingStop() {
        super.onStreamingStop()
        // Stop the media projection
        stopMediaProjection()
    }

    /**
     * Creates the default video source.
     *
     * The default video source is a [MediaProjectionVideoSourceFactory].
     *
     * @param mediaProjection the media projection
     * @param extras the extras bundle
     * @return the video source factory
     */
    protected open fun createDefaultVideoSource(
        mediaProjection: MediaProjection,
        extras: Bundle
    ): IVideoSourceInternal.Factory? {
        return MediaProjectionVideoSourceFactory(mediaProjection)
    }

    /**
     * Creates the default audio source.
     *
     * @param mediaProjection the media projection
     * @param extras the extras bundle
     * @return the audio source factory
     */
    protected abstract fun createDefaultAudioSource(
        mediaProjection: MediaProjection, extras: Bundle
    ): IAudioSourceInternal.Factory?

    /**
     * Sets the media projection to the streamer.
     *
     * This method is called when [onBind] is called.
     *
     * You can use it to set the audio and video sources
     *
     * @param streamer the streamer of the service
     * @param mediaProjection the media projection to set
     * @param extras the extras bundle
     */
    protected open suspend fun setStreamerMediaProjection(
        streamer: T,
        mediaProjection: MediaProjection,
        extras: Bundle
    ) {
        require(streamer is IWithVideoSource || streamer is IWithAudioSource) {
            "Streamer must implement IWithVideoSource or IWithAudioSource"
        }

        if (streamer is IWithVideoSource) {
            val videoSource = streamer.videoInput?.sourceFlow?.value
            if (videoSource is IMediaProjectionSource) {
                streamer.setVideoSource(MediaProjectionVideoSourceFactory(mediaProjection))
            } else if (videoSource == null) {
                createDefaultVideoSource(
                    mediaProjection,
                    extras
                )?.let { streamer.setVideoSource(it) }
            }
        }

        if (streamer is IWithAudioSource) {
            val audioSource = streamer.audioInput?.sourceFlow?.value
            if (audioSource is IMediaProjectionSource) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    streamer.setAudioSource(MediaProjectionAudioSourceFactory(mediaProjection))
                } else {
                    throw UnsupportedOperationException(
                        "Media projection audio source is not supported on this version of Android"
                    )
                }
            } else if (audioSource == null) {
                createDefaultAudioSource(
                    mediaProjection,
                    extras
                )?.let { streamer.setAudioSource(it) }
            }
        }
    }

    override suspend fun onExtra(extras: Bundle) {
        setStreamerMediaProjection(streamer, createMediaProjectionFromBundle(extras), extras)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop the media projection
        stopMediaProjection()
    }

    private fun stopMediaProjection() {
        mediaProjection?.stop()
        mediaProjection = null
    }

    companion object {
        private const val DEFAULT_NOTIFICATION_CHANNEL_ID =
            "io.github.thibaultbee.streampack.services.mediaprojection"
        private const val DEFAULT_NOTIFICATION_ID = 3783

        // Activity result
        private const val ACTIVITY_RESULT_KEY = "activityResult"
        private const val RESULT_CODE_KEY = "resultCode"
        private const val RESULT_DATA_KEY = "resultData"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [MediaProjectionService].
         * @param resultCode the result code of the [ActivityResult]
         * @param resultData the result data of the [ActivityResult]
         * @param onServiceCreated the callback that returns a children of [MediaProjectionService] instance when the service has been connected.
         * @param onServiceDisconnected the callback that will be called when the service is disconnected.
         * @param onExtra the callback that will be called to pass the extra bundle to the service
         * @return the service connection. Use it to [Context.unbindService] when you don't need the service anymore.
         */
        fun bindService(
            context: Context,
            serviceClass: Class<out MediaProjectionService<*>>,
            resultCode: Int,
            resultData: Intent,
            onServiceCreated: (IStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit = {},
            onExtra: (Intent) -> Unit = {}
        ): ServiceConnection {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    if (service is StreamerService<*>.StreamerServiceBinder) {
                        onServiceCreated(service.streamer)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onServiceDisconnected(name)
                }
            }

            bindService(
                context,
                serviceClass,
                resultCode,
                resultData,
                connection,
                onExtra
            )

            return connection
        }

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [MediaProjectionService].
         * @param resultCode the result code of the [ActivityResult]
         * @param resultData the result data of the [ActivityResult]
         * @param connection the service connection.
         * @param onExtra the callback that will be called to pass the extra bundle to the service.
         */
        private fun bindService(
            context: Context,
            serviceClass: Class<out MediaProjectionService<*>>,
            resultCode: Int,
            resultData: Intent,
            connection: ServiceConnection,
            onExtra: (Intent) -> Unit = {}
        ) {
            require(resultCode == Activity.RESULT_OK) {
                "Result code must be Activity.RESULT_OK"
            }

            val activityResultBundle = Bundle().apply {
                putInt(RESULT_CODE_KEY, resultCode)
                putParcelable(RESULT_DATA_KEY, resultData)
            }

            bindService(
                context, serviceClass, connection
            ) { extra ->
                onExtra(extra)
                // Pass the extra bundle to the service
                extra.putExtra(ACTIVITY_RESULT_KEY, activityResultBundle)
            }
        }
    }
}