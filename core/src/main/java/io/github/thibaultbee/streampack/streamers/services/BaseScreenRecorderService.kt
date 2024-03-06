/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.streamers.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import io.github.thibaultbee.streampack.R
import io.github.thibaultbee.streampack.internal.utils.extensions.rootCause
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.logger.Logger
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer
import io.github.thibaultbee.streampack.utils.NotificationUtils
import io.github.thibaultbee.streampack.utils.getStreamer
import kotlinx.coroutines.runBlocking

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
 *   - override open onNotification methods: [onConnectionSuccessNotification], [onConnectionLostNotification], [onConnectionFailedNotification] and [onErrorNotification]
 *
 * If you want to keep the notification, you shall not override [BaseScreenRecorderStreamer.onErrorListener] and [ILiveStreamer.onConnectionListener].
 *
 * @param notificationId the notification id a unique number
 * @param channelId the notification channel id
 * @param channelNameResourceId A string resource identifier for the user visible name of the notification channel.
 * @param channelDescriptionResourceId A string resource identifier for the user visible description of the notification channel.
 * @param notificationIconResourceId A drawable resource identifier for the user visible icon of the notification channel.
 */
abstract class BaseScreenRecorderService(
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    protected val channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    @StringRes protected val channelNameResourceId: Int = R.string.default_channel_name,
    @StringRes protected val channelDescriptionResourceId: Int = 0,
    @DrawableRes protected val notificationIconResourceId: Int = R.drawable.ic_baseline_linked_camera_24
) : Service() {
    protected var streamer: BaseScreenRecorderStreamer? = null
    private val binder = ScreenRecorderServiceBinder()
    private val notificationUtils: NotificationUtils by lazy {
        NotificationUtils(this, channelId, notificationId)
    }
    private var hasNotified = false

    private val liveStreamer: ILiveStreamer?
        get() = streamer?.getStreamer<ILiveStreamer>()

    override fun onCreate() {
        super.onCreate()

        notificationUtils.createNotificationChannel(
            channelNameResourceId,
            channelDescriptionResourceId
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        try {
            val constructorBundle = intent.extras?.getBundle(CONSTRUCTOR_BUNDLE_KEY)
                ?: throw IllegalStateException("Config bundle must be pass to the service")

            val enableAudio = constructorBundle.getBoolean(ENABLE_AUDIO_KEY)
            if (enableAudio) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    throw SecurityException("Permission RECORD_AUDIO must have been granted!")
                }
            }

            streamer = createStreamer(constructorBundle)

            streamer?.onErrorListener = object : OnErrorListener {
                override fun onError(e: Exception) {
                    Logger.e(TAG, "An error occurred", e)
                    onErrorNotification(e)?.let { notify(it) }
                    stopSelf()
                }
            }

            liveStreamer?.onConnectionListener =
                object : OnConnectionListener {
                    override fun onLost(message: String) {
                        Logger.e(TAG, "Connection lost: $message")
                        onConnectionLostNotification(message)?.let { notify(it) }
                        stopSelf()
                    }

                    override fun onFailed(message: String) {
                        Logger.e(TAG, "Connection failed: $message")
                        onConnectionFailedNotification(message)?.let { notify(it) }
                        stopSelf()
                    }

                    override fun onSuccess() {
                        Logger.i(TAG, "Connection succeeded")
                        onConnectionSuccessNotification()?.let { notify(it) }
                    }
                }
        } catch (e: Exception) {
            Logger.e(TAG, "An error occurred", e)
            onErrorNotification(e)?.let { notify(it) }
            stopSelf()
        }

        return binder
    }

    /**
     * Calls when service needs to create the streamer.
     *
     * @return the streamer to use.
     */
    protected abstract fun createStreamer(bundle: Bundle): BaseScreenRecorderStreamer

    override fun onDestroy() {
        super.onDestroy()
        runBlocking {
            streamer?.stopStream()
        }
        (streamer as ILiveStreamer?)?.disconnect()
        streamer?.release()
        streamer = null
    }

    /**
     * Called when an error happens
     *
     * You can override this method to customize the notification.
     *
     * @param e the exception that was thrown
     */
    protected open fun onErrorNotification(e: Exception): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_error),
            e.rootCause.localizedMessage,
            notificationIconResourceId
        )
    }

    /**
     * Called when connection was lost
     *
     * You can override this method to customize the notification.
     *
     * @param message the reason why the connection was lost
     */
    protected open fun onConnectionLostNotification(message: String): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_connection_lost),
            message,
            notificationIconResourceId
        )
    }

    /**
     * Called when connection failed to be established
     *
     * You can override this method to customize the notification.
     *
     * @param message the reason why the connection failed to be established
     */
    protected open fun onConnectionFailedNotification(message: String): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_connection_failed),
            message,
            notificationIconResourceId
        )
    }

    /**
     * Called when connection succeeded
     *
     * You can override this method to customize the notification.
     */
    protected open fun onConnectionSuccessNotification(): Notification? {
        return notificationUtils.createNotification(
            R.string.service_notification_connection_succeeded,
            0,
            notificationIconResourceId
        )
    }

    /**
     * Notify the user with a notification.
     *
     * @param notification the notification to display
     */
    private fun notify(notification: Notification) {
        if (!hasNotified) {
            startForeground(
                notificationId,
                notification
            )
            hasNotified = true
        } else {
            notificationUtils.notify(notification)
        }
    }

    protected inner class ScreenRecorderServiceBinder : Binder() {
        val streamer: BaseScreenRecorderStreamer?
            get() = this@BaseScreenRecorderService.streamer
    }

    companion object {
        private const val TAG = "BaseScreenRecorderService"

        const val DEFAULT_NOTIFICATION_CHANNEL_ID =
            "io.github.thibaultbee.streampack.streamers.services"
        const val DEFAULT_NOTIFICATION_ID = 3782

        private const val CONSTRUCTOR_BUNDLE_KEY = "config"
        const val ENABLE_AUDIO_KEY = "enableAudio"
        const val MUXER_CONFIG_KEY = "muxerConfig"
        const val ENDPOINT_CONFIG_KEY = "endpointConfig"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context The application context.
         * @param serviceClass The service class to launch. It is a children of [BaseScreenRecorderService].
         * @param enableAudio enable or disable audio.
         * @param onServiceCreated Callback that returns a children of [BaseScreenRecorderService] instance when the service has been connected.
         * @param onServiceDisconnected Callback that will be called when the service is disconnected.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out BaseScreenRecorderService>,
            enableAudio: Boolean,
            onServiceCreated: (BaseScreenRecorderStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit
        ) {
            launch(
                context,
                serviceClass,
                Bundle().apply { putBoolean(ENABLE_AUDIO_KEY, enableAudio) },
                onServiceCreated,
                onServiceDisconnected
            )
        }

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context The application context.
         * @param serviceClass The service class to launch. It is a children of [BaseScreenRecorderService].
         * @param constructorBundle the streamer constructor configuration [Bundle].
         * @param onServiceCreated Callback that returns a children of [BaseScreenRecorderService] instance when the service has been connected.
         * @param onServiceDisconnected Callback that will be called when the service is disconnected.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out BaseScreenRecorderService>,
            constructorBundle: Bundle,
            onServiceCreated: (BaseScreenRecorderStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit
        ) {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    if (service is BaseScreenRecorderService.ScreenRecorderServiceBinder) {
                        service.streamer?.let { onServiceCreated(it) }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onServiceDisconnected(name)
                }
            }

            val intent = Intent(context, serviceClass).apply {
                putExtra(CONSTRUCTOR_BUNDLE_KEY, constructorBundle)
            }

            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }
}