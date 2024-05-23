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
package io.github.thibaultbee.streampack.services.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat

/**
 * Helper class to create and manage notifications.
 * Only use for screen recording service that is why the permission error is suppressed.
 */
@Suppress("NotificationPermission")
class NotificationUtils(
    private val service: Service,
    private val channelId: String,
    private val notificationId: Int
) {
    private val notificationManager: NotificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private var hasNotified = false

    fun cancel() {
        service.stopForeground(true)
        hasNotified = false
    }

    fun notify(notification: Notification) {
        if (!hasNotified) {
            service.startForeground(
                notificationId,
                notification
            )
            hasNotified = true
        } else {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun createNotification(
        @StringRes titleResourceId: Int,
        @StringRes contentResourceId: Int,
        @DrawableRes iconResourceId: Int
    ): Notification {
        return createNotification(
            service.getString(titleResourceId),
            if (contentResourceId != 0) {
                service.getString(contentResourceId)
            } else {
                null
            },
            iconResourceId
        )
    }

    fun createNotification(
        title: String,
        content: String? = null,
        @DrawableRes iconResourceId: Int
    ): Notification {
        val builder = NotificationCompat.Builder(service, channelId).apply {
            setSmallIcon(iconResourceId)
            setContentTitle(title)

            content?.let {
                setContentText(it)
            }
        }

        return builder.build()
    }

    fun createNotificationChannel(
        @StringRes nameResourceId: Int,
        @StringRes descriptionResourceId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val name = service.getString(nameResourceId)

            val channel = NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            if (descriptionResourceId != 0) {
                channel.description = service.getString(descriptionResourceId)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}