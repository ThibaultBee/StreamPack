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
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.utils.extensions.rootCause
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.streamers.orientation.IRotationProvider
import io.github.thibaultbee.streampack.core.streamers.orientation.SensorRotationProvider
import io.github.thibaultbee.streampack.core.streamers.single.ScreenRecorderSingleStreamer
import io.github.thibaultbee.streampack.services.utils.NotificationUtils
import kotlinx.coroutines.flow.filterNotNull
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
 *   - override open onNotification methods: [onOpenNotification], [onCloseNotification] and [onErrorNotification]
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
) : LifecycleService(), IRotationProvider.Listener {
    protected var streamer: ScreenRecorderSingleStreamer? = null
        private set

    protected open val rotationProvider: IRotationProvider by lazy { SensorRotationProvider(this) }

    private val binder = ScreenRecorderServiceBinder()
    private val notificationUtils: NotificationUtils by lazy {
        NotificationUtils(this, channelId, notificationId)
    }

    override fun onCreate() {
        super.onCreate()

        rotationProvider.addListener(this)

        notificationUtils.createNotificationChannel(
            channelNameResourceId,
            channelDescriptionResourceId
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
            val customBundle = intent.extras?.getBundle(CUSTOM_BUNDLE_KEY)
                ?: throw IllegalStateException("Config bundle must be pass to the service")

            streamer = createStreamer(customBundle).apply {
                lifecycleScope.launch {
                    throwable.filterNotNull().collect { t ->
                        Logger.e(TAG, "An error occurred", t)
                        onErrorNotification(t)?.let { notify(it) }
                        stopSelf()
                    }
                }
                lifecycleScope.launch {
                    isOpen.collect { isOpen ->
                        if (isOpen) {
                            Logger.i(TAG, "Open succeeded")
                            onOpenNotification()?.let { notify(it) }
                        } else {
                            onCloseNotification()?.let { notify(it) }
                            Logger.w(TAG, "Closed")
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
     * Calls when service needs to create the streamer.
     * You can customize the streamer by overriding this method.
     *
     * @param customBundle the custom bundle passed as [launch] parameter.
     * @return the streamer to use.
     */
    open fun createStreamer(customBundle: Bundle): ScreenRecorderSingleStreamer {
        val enableMicrophone = customBundle.getBoolean(ENABLE_MICROPHONE_KEY, false)
        if (enableMicrophone) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Permission RECORD_AUDIO must have been granted!")
            }
        }

        return ScreenRecorderSingleStreamer(
            applicationContext,
            enableMicrophone = enableMicrophone,
        )
    }

    override fun onOrientationChanged(rotation: Int) {
        streamer?.targetRotation = rotation
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationUtils.cancel()

        rotationProvider.removeListener(this)

        runBlocking {
            streamer?.stopStream()
            streamer?.close()
        }
        streamer?.release()
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
            R.string.service_notification_streamer_open,
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
        notificationUtils.notify(notification)
    }

    protected inner class ScreenRecorderServiceBinder : Binder() {
        val streamer: ScreenRecorderSingleStreamer?
            get() = this@DefaultScreenRecorderService.streamer
    }

    companion object {
        private const val TAG = "ScreenRecorderService"

        const val DEFAULT_NOTIFICATION_CHANNEL_ID =
            "io.github.thibaultbee.streampack.streamers.services"
        const val DEFAULT_NOTIFICATION_ID = 3782

        // Config
        private const val CUSTOM_BUNDLE_KEY = "bundle"
        private const val ENABLE_MICROPHONE_KEY = "enableMicrophone"

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [DefaultScreenRecorderService].
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         * @param onServiceCreated the callback that returns a children of [DefaultScreenRecorderService] instance when the service has been connected.
         * @param onServiceDisconnected the callback that will be called when the service is disconnected.
         * @param enableAudio enable audio recording.
         * @return the service connection. Use it to [Context.unbindService] when you don't need the service anymore.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out DefaultScreenRecorderService>,
            onServiceCreated: (ScreenRecorderSingleStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit = {},
            customBundle: Bundle = Bundle(),
            enableAudio: Boolean = false
        ): ServiceConnection {
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

            launch(context, serviceClass, connection, customBundle, enableAudio)

            return connection
        }

        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context the application context.
         * @param serviceClass the service class to launch. It is a children of [DefaultScreenRecorderService].
         * @param connection the service connection.
         * @param customBundle the user defined [Bundle]. It will be passed to the service. You can retrieve it in [createStreamer] method.
         * @param enableAudio enable audio recording.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out DefaultScreenRecorderService>,
            connection: ServiceConnection,
            customBundle: Bundle = Bundle(),
            enableAudio: Boolean = false
        ) {
            customBundle.putBoolean(ENABLE_MICROPHONE_KEY, enableAudio)
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