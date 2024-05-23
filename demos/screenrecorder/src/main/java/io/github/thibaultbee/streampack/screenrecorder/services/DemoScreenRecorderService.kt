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
package io.github.thibaultbee.streampack.screenrecorder.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.screenrecorder.R
import io.github.thibaultbee.streampack.screenrecorder.models.Actions
import io.github.thibaultbee.streampack.services.DefaultScreenRecorderService
import io.github.thibaultbee.streampack.streamers.DefaultScreenRecorderStreamer
import kotlinx.coroutines.launch

class DemoScreenRecorderService : DefaultScreenRecorderService(
    notificationId = 0x4569,
    channelId = "io.github.thibaultbee.streampack.screenrecorder.demo",
    channelNameResourceId = R.string.app_name
) {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            streamer?.let {
                if (intent.action == Actions.STOP.value) {
                    lifecycleScope.launch {
                        streamer?.stopStream()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Create a [DefaultScreenRecorderStreamer] with the custom [Bundle].
     */
    override fun createStreamer(bundle: Bundle): DefaultScreenRecorderStreamer {
        val enableMicrophone = bundle.getBoolean(ENABLE_MICROPHONE_KEY, false)
        if (enableMicrophone) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Permission RECORD_AUDIO must have been granted!")
            }
        }

        return DefaultScreenRecorderStreamer(
            applicationContext,
            enableMicrophone = enableMicrophone,
        )
    }

    override fun onConnectionSuccessNotification(): Notification {
        val intent =
            Intent(this, DemoScreenRecorderService::class.java).setAction(Actions.STOP.value)
        val stopIntent =
            PendingIntent.getService(this, 5678, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(notificationIconResourceId)
            .setContentTitle(getString(R.string.live_in_progress))
            .addAction(
                R.drawable.ic_baseline_stop_24,
                getString(R.string.stop),
                stopIntent
            )
            .build()
    }

    companion object {
        // Config
        const val ENABLE_MICROPHONE_KEY = "enableMicrophone"
    }
}