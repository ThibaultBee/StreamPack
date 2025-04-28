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

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rootCause
import io.github.thibaultbee.streampack.core.interfaces.ICloseableStreamer
import io.github.thibaultbee.streampack.core.interfaces.IStreamer
import io.github.thibaultbee.streampack.core.interfaces.IWithVideoRotation
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.orientation.IRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.SensorRotationProvider
import io.github.thibaultbee.streampack.services.utils.NotificationUtils
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that manages streamers.
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
abstract class StreamerService<T : IStreamer>(
    private val notificationId: Int = DEFAULT_NOTIFICATION_ID,
    protected val channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    @StringRes protected val channelNameResourceId: Int = R.string.default_channel_name,
    @StringRes protected val channelDescriptionResourceId: Int = 0,
    @DrawableRes protected val notificationIconResourceId: Int = R.drawable.ic_baseline_linked_camera_24
) : LifecycleService(), IRotationProvider.Listener {
    protected var streamer: T? = null
        private set

    protected open val rotationProvider: IRotationProvider? by lazy { SensorRotationProvider(this) }

    private val binder = StreamerServiceBinder()
    private val notificationUtils: NotificationUtils by lazy {
        NotificationUtils(this, channelId, notificationId)
    }

    override fun onCreate() {
        super.onCreate()

        rotationProvider?.addListener(this)

        notificationUtils.createNotificationChannel(
            channelNameResourceId, channelDescriptionResourceId
        )

        ServiceCompat.startForeground(
            this,
            notificationId,
            onCreateNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        try {
            val extras = intent.extras
                ?: throw IllegalStateException("Config bundle must be pass to the service")

            val streamer = runBlocking { createStreamer(extras) }.apply {
                this@StreamerService.streamer = this
            }

            lifecycleScope.launch {
                streamer.throwableFlow.filterNotNull().collect { t ->
                    Logger.e(TAG, "An error occurred", t)
                    onErrorNotification(t)?.let { notify(it) }
                    stopSelf()
                }
            }

            if (streamer is ICloseableStreamer) {
                lifecycleScope.launch {
                    streamer.isOpenFlow.collect { isOpen ->
                        if (isOpen) {
                            Logger.i(TAG, "Open succeeded")
                            onOpenNotification()?.let { notify(it) }
                        } else {
                            Logger.w(TAG, "Closed")
                            onCloseNotification()?.let { notify(it) }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "An error occurred", t)
            onErrorNotification(t)?.let { notify(it) }
            stopSelf()
        }
        return binder
    }

    /**
     * Creates the streamer from the [Intent.getExtras] passed in the [Intent].
     *
     * It is called when the service is created.
     *
     * @param extras the bundle passed in the [Intent.getExtras] pass to [bindService]
     * @return the streamer to use.
     */
    protected abstract suspend fun createStreamer(
        extras: Bundle
    ): T

    override fun onOrientationChanged(rotation: Int) {
        lifecycleScope.launch {
            (streamer as? IWithVideoRotation)?.setTargetRotation(rotation)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationUtils.cancel()

        rotationProvider?.removeListener(this)

        runBlocking {
            streamer?.stopStream()
            (streamer as ICloseableStreamer?)?.close()
            streamer?.release()
        }
        streamer = null
        Log.i(TAG, "Service destroyed")
    }

    /**
     * Called when the service is created
     *
     * You can override this method to customize the notification.
     */
    protected open fun onCreateNotification(): Notification {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_service_created),
            null,
            notificationIconResourceId
        )
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
            getString(R.string.service_notification_streamer_error),
            t.rootCause.localizedMessage,
            notificationIconResourceId
        )
    }

    /**
     * Called when streamer is closed
     *
     * You can override this method to customize the notification.
     */
    protected open fun onCloseNotification(): Notification? {
        return notificationUtils.createNotification(
            getString(R.string.service_notification_streamer_closed),
            null,
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
            R.string.service_notification_streamer_open, 0, notificationIconResourceId
        )
    }

    /**
     * Notify the user with a notification.
     *
     * @param notification the notification to display
     */
    private fun notify(notification: Notification) {
        notificationUtils.notify(notification)
    }

    protected inner class StreamerServiceBinder : Binder() {
        val streamer: T?
            get() = this@StreamerService.streamer
    }

    companion object {
        private const val TAG = "StreamerService"

        // Notification
        private const val DEFAULT_NOTIFICATION_CHANNEL_ID =
            "io.github.thibaultbee.streampack.services.streamer"
        private const val DEFAULT_NOTIFICATION_ID = 3782

        // Config
        const val USER_BUNDLE_KEY = "bundle"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [StreamerService].
         * @param onServiceCreated the callback that returns a children of [StreamerService] instance when the service has been connected.
         * @param onServiceDisconnected the callback that will be called when the service is disconnected.
         * @param customBundleName the name of the bundle to pass to the service. It is used to retrieve the bundle in [createStreamer] method.
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         * @return the service connection. Use it to [Context.unbindService] when you don't need the service anymore.
         */
        fun bindService(
            context: Context,
            serviceClass: Class<out StreamerService<*>>,
            onServiceCreated: (IStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit = {},
            customBundleName: String = USER_BUNDLE_KEY,
            customBundle: Bundle = Bundle()
        ): ServiceConnection {
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                    if (service is StreamerService<*>.StreamerServiceBinder) {
                        service.streamer?.let { onServiceCreated(it) }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onServiceDisconnected(name)
                }
            }

            bindService(
                context, serviceClass, connection, customBundleName, customBundle
            )

            return connection
        }

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [StreamerService].
         * @param connection the service connection.
         * @param customBundleName the name of the bundle to pass to the service. It is used to retrieve the bundle in [createStreamer] method.
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         */
        fun bindService(
            context: Context,
            serviceClass: Class<out StreamerService<*>>,
            connection: ServiceConnection,
            customBundleName: String = USER_BUNDLE_KEY,
            customBundle: Bundle = Bundle()
        ) {
            val intent = Intent(context, serviceClass).apply {
                putExtra(customBundleName, customBundle)
            }

            context.bindService(
                intent, connection, Context.BIND_AUTO_CREATE
            )
        }
    }
}