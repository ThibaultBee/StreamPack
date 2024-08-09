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
package io.github.thibaultbee.streampack.services

import android.Manifest
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.internal.utils.extensions.rootCause
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.DefaultScreenRecorderStreamer
import io.github.thibaultbee.streampack.services.utils.NotificationUtils
import kotlinx.coroutines.launch
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
 *   - override open onNotification methods: [onOpenNotification], [onCloseNotification], [onOpenFailedNotification] and [onErrorNotification]
 *
 * If you want to keep the notification, you shall not override [DefaultScreenRecorderStreamer.onErrorListener] and [ILiveStreamer.onConnectionListener].
 *
 * @param notificationId the notification id a unique number
 * @param channelId the notification channel id
 * @param channelNameResourceId A string resource identifier for the user visible name of the notification channel.
 * @param channelDescriptionResourceId A string resource identifier for the user visible description of the notification channel.
 * @param notificationIconResourceId A drawable resource identifier for the user visible icon of the notification channel.
 */
abstract class DefaultScreenRecorderService(
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    protected val channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    @StringRes protected val channelNameResourceId: Int = R.string.default_channel_name,
    @StringRes protected val channelDescriptionResourceId: Int = 0,
    @DrawableRes protected val notificationIconResourceId: Int = R.drawable.ic_baseline_linked_camera_24
) : LifecycleService() {
    protected var streamer: DefaultScreenRecorderStreamer? = null
    private val binder = ScreenRecorderServiceBinder()
    private val notificationUtils: NotificationUtils by lazy {
        NotificationUtils(this, channelId, notificationId)
    }
    private var isStarted = false

    override fun onCreate() {
        super.onCreate()

        notificationUtils.createNotificationChannel(
            channelNameResourceId,
            channelDescriptionResourceId
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        try {
            val customBundle = intent.extras?.getBundle(CUSTOM_BUNDLE_KEY)
                ?: throw IllegalStateException("Config bundle must be pass to the service")

            streamer = createStreamer(customBundle)

            streamer?.let {
                lifecycleScope.launch {
                    it.throwable.collect { t ->
                        if (t != null) {
                            Logger.e(TAG, "An error occurred", t)
                            onErrorNotification(t)?.let { notify(it) }
                            stopSelf()
                        }
                    }
                }
                lifecycleScope.launch {
                    it.isOpened.collect { isOpened ->
                        if (isOpened) {
                            Logger.i(TAG, "Open succeeded")
                            onOpenNotification()?.let { notify(it) }
                        } else {
                            val message = "Closed" // TODO
                            Logger.e(TAG, "Closed: $message")
                            onCloseNotification(message)?.let { notify(it) }
                            stopSelf()
                        }
                    }
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
     * You can customize the streamer by overriding this method.
     *
     * @param bundle the custom bundle passed as [launch] parameter.
     * @return the streamer to use.
     */
    open fun createStreamer(bundle: Bundle): DefaultScreenRecorderStreamer {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Permission RECORD_AUDIO must have been granted!")
        }

        return DefaultScreenRecorderStreamer(
            applicationContext,
            enableMicrophone = true,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        runBlocking {
            streamer?.stopStream()
            streamer?.close()
        }
        streamer?.release()
        streamer = null
    }

    /**
     * Called when an error happens
     *
     * You can override this method to customize the notification.
     *
     * @param t the throwable that was thrown
     */
    protected open fun onErrorNotification(t: Throwable): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_error),
            t.rootCause.localizedMessage,
            notificationIconResourceId
        )
    }

    /**
     * Called when streamer is closed
     *
     * You can override this method to customize the notification.
     *
     * @param message the reason why the streamer is closed
     */
    protected open fun onCloseNotification(message: String): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_closed),
            message,
            notificationIconResourceId
        )
    }

    /**
     * Called when the stream failed to open
     *
     * You can override this method to customize the notification.
     *
     * @param message the reason why the open failed
     */
    protected open fun onOpenFailedNotification(message: String): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_open_failed),
            message,
            notificationIconResourceId
        )
    }

    /**
     * Called when the streamer is opened
     *
     * You can override this method to customize the notification.
     */
    protected open fun onOpenNotification(): Notification? {
        return notificationUtils.createNotification(
            R.string.service_notification_open,
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
        if (!isStarted) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.foregroundServiceType
                } else {
                    0
                }
            )
            isStarted = true
        } else {
            notificationUtils.notify(notification)
        }
    }

    protected inner class ScreenRecorderServiceBinder : Binder() {
        val streamer: DefaultScreenRecorderStreamer?
            get() = this@DefaultScreenRecorderService.streamer
    }

    companion object {
        private const val TAG = "DefaultScreenRecorderService"

        const val DEFAULT_NOTIFICATION_CHANNEL_ID =
            "io.github.thibaultbee.streampack.streamers.services"
        const val DEFAULT_NOTIFICATION_ID = 3782

        private const val CUSTOM_BUNDLE_KEY = "bundle"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context The application context.
         * @param serviceClass The service class to launch. It is a children of [DefaultScreenRecorderService].
         * @param customBundle the user defined [Bundle].
         * @param onServiceCreated Callback that returns a children of [DefaultScreenRecorderService] instance when the service has been connected.
         * @param onServiceDisconnected Callback that will be called when the service is disconnected.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out DefaultScreenRecorderService>,
            customBundle: Bundle,
            onServiceCreated: (DefaultScreenRecorderStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit
        ) {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    if (service is ScreenRecorderServiceBinder) {
                        service.streamer?.let { onServiceCreated(it) }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onServiceDisconnected(name)
                }
            }

            val intent = Intent(context, serviceClass).apply {
                putExtra(CUSTOM_BUNDLE_KEY, customBundle)
            }

            context.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }
}