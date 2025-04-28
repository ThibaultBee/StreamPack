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
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.IVideoStreamer
import io.github.thibaultbee.streampack.core.utils.extensions.getMediaProjection
import io.github.thibaultbee.streampack.services.MediaProjectionService.Companion.bindService
import io.github.thibaultbee.streampack.services.StreamerService.Companion.bindService
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Foreground service that manages screen recorder streamers.
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
 * @param notificationId the notification id a unique number
 * @param channelId the notification channel id
 * @param channelNameResourceId A string resource identifier for the user visible name of the notification channel.
 * @param channelDescriptionResourceId A string resource identifier for the user visible description of the notification channel.
 * @param notificationIconResourceId A drawable resource identifier for the user visible icon of the notification channel.
 */
abstract class MediaProjectionService<T : IVideoStreamer<*>>(
    notificationId: Int = DEFAULT_NOTIFICATION_ID,
    channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    @StringRes channelNameResourceId: Int = R.string.default_channel_name,
    @StringRes channelDescriptionResourceId: Int = 0,
    @DrawableRes notificationIconResourceId: Int = R.drawable.ic_baseline_linked_camera_24
) : StreamerService<T>(
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

    /**
     * Creates the media projection streamer.
     *
     * It is called when the service is created.
     *
     * @param extras the bundle passed in the [Intent.getExtras] pass to [bindService]
     * @param resultCode the result code of the [ActivityResult]
     * @param resultData the result data of the [ActivityResult]
     * @return the streamer to use.
     */
    private suspend fun createMediaProjectionStreamer(
        extras: Bundle,
        resultCode: Int,
        resultData: Intent
    ): T {
        val mediaProjection = applicationContext.getMediaProjection(resultCode, resultData).also {
            this@MediaProjectionService.mediaProjection = it
        }
        return createMediaProjectionStreamer(extras, mediaProjection)
    }

    /**
     * Creates the media projection streamer.
     *
     * It is called when the service is created.
     *
     * @param extras the bundle passed in the [Intent.getExtras] pass to [bindService]
     * @param mediaProjection the media projection. It is stopped when the service is destroyed.
     * @return the streamer to use.
     */
    protected abstract suspend fun createMediaProjectionStreamer(
        extras: Bundle,
        mediaProjection: MediaProjection
    ): T

    override suspend fun createStreamer(extras: Bundle): T {
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

        val streamer = createMediaProjectionStreamer(extras, resultCode, resultData)
        lifecycleScope.launch {
            streamer.isStreamingFlow.drop(1).collect { isStreaming ->
                if (!isStreaming) {
                    Logger.d(TAG, "Streamer stopped")
                    onStreamStopped()
                }
            }
        }
        return streamer
    }

    /**
     * Called when the streamer is stopped.
     *
     * By default, it stops the media projection but you can override it to do something else.
     */
    protected open fun onStreamStopped() {
        // Stop the media projection
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop the media projection
        mediaProjection?.stop()
        mediaProjection = null
    }

    companion object {
        private const val TAG = "MediaProjectionService"

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
         * @param customBundleName the name of the bundle to pass to the service. It is used to retrieve the bundle in [createStreamer] method.
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         * @return the service connection. Use it to [Context.unbindService] when you don't need the service anymore.
         */
        fun bindService(
            context: Context,
            serviceClass: Class<out MediaProjectionService<*>>,
            resultCode: Int,
            resultData: Intent,
            onServiceCreated: (IVideoStreamer<*>) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit = {},
            customBundleName: String = USER_BUNDLE_KEY,
            customBundle: Bundle = Bundle()
        ): ServiceConnection {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    if (service is StreamerService<*>.StreamerServiceBinder) {
                        service.streamer?.let { onServiceCreated(it as IVideoStreamer<*>) }
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
                customBundleName,
                customBundle
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
         * @param customBundleName the name of the bundle to pass to the service. It is used to retrieve the bundle in [createStreamer] method.
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         */
        private fun bindService(
            context: Context,
            serviceClass: Class<out MediaProjectionService<*>>,
            resultCode: Int,
            resultData: Intent,
            connection: ServiceConnection,
            customBundleName: String = USER_BUNDLE_KEY,
            customBundle: Bundle = Bundle(),
        ) {
            require(resultCode == Activity.RESULT_OK) {
                "Result code must be Activity.RESULT_OK"
            }

            val activityResultBundle = Bundle().apply {
                putInt(RESULT_CODE_KEY, resultCode)
                putParcelable(RESULT_DATA_KEY, resultData)
            }
            val intent = Intent(context, serviceClass).apply {
                putExtra(customBundleName, customBundle)
                putExtra(ACTIVITY_RESULT_KEY, activityResultBundle)
            }

            context.bindService(
                intent, connection, Context.BIND_AUTO_CREATE
            )
        }
    }
}