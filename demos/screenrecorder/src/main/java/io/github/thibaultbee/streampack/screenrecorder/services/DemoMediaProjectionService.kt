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

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import io.github.thibaultbee.streampack.core.elements.sources.audio.IAudioSourceInternal
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MediaProjectionAudioSourceFactory
import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.MicrophoneSourceFactory
import io.github.thibaultbee.streampack.core.interfaces.ICloseableStreamer
import io.github.thibaultbee.streampack.core.streamers.dual.IVideoDualStreamer
import io.github.thibaultbee.streampack.core.streamers.orientation.IRotationProvider
import io.github.thibaultbee.streampack.core.streamers.single.ISingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.IVideoSingleStreamer
import io.github.thibaultbee.streampack.core.streamers.single.audioVideoMediaProjectionSingleStreamer
import io.github.thibaultbee.streampack.screenrecorder.R
import io.github.thibaultbee.streampack.screenrecorder.models.Actions
import io.github.thibaultbee.streampack.services.MediaProjectionService
import io.github.thibaultbee.streampack.services.utils.SingleStreamerFactory
import kotlinx.coroutines.launch

/**
 * A [MediaProjectionService] implementation to demonstrate how to create your own service
 * [audioVideoMediaProjectionSingleStreamer].
 *
 * The [MediaProjectionService] expects [IVideoSingleStreamer] or [IVideoDualStreamer] as generic
 * type.
 */
class DemoMediaProjectionService : MediaProjectionService<ISingleStreamer>(
    streamerFactory = SingleStreamerFactory(
        withAudio = true,
        withVideo = true
    ),
    notificationId = 0x4569,
    channelId = "io.github.thibaultbee.streampack.screenrecorder.demo",
    channelNameResourceId = R.string.app_name
) {
    /**
     * Overwrite the [IRotationProvider] to change orientation behavior.
     */
    //override val rotationProvider: IRotationProvider? = null

    /**
     * Override to use another audio source.
     */
    override fun createDefaultAudioSource(
        mediaProjection: MediaProjection,
        extras: Bundle
    ): IAudioSourceInternal.Factory {
        val audioSource = extras.getString(AUDIO_SOURCE_KEY)
        return if (audioSource == AUDIO_SOURCE_MEDIA_PROJECTION_KEY) {
            /**
             * For audio playback as audio source.
             */

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaProjectionAudioSourceFactory(mediaProjection)
            } else {
                throw UnsupportedOperationException(
                    "Media projection audio source is not supported on this version of Android"
                )
            }
        } else if (audioSource == AUDIO_SOURCE_MICROPHONE_KEY) {
            /**
             * For microphone as audio source.
             */
            MicrophoneSourceFactory()
        } else {
            throw IllegalArgumentException(
                "Audio source $audioSource is not supported. Use $AUDIO_SOURCE_MEDIA_PROJECTION_KEY or $AUDIO_SOURCE_MICROPHONE_KEY"
            )
        }
    }

    /**
     * Override the [onStartCommand] to handle the stop action.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            streamer.let {
                if (intent.action == Actions.STOP.value) {
                    lifecycleScope.launch {
                        streamer.stopStream()
                        (streamer as? ICloseableStreamer)?.close()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * A custom notification with a stop action.
     */
    override fun onOpenNotification(): Notification {
        val intent =
            Intent(this, DemoMediaProjectionService::class.java).setAction(Actions.STOP.value)
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
        internal const val AUDIO_SOURCE_KEY = "audioSource"
        internal const val AUDIO_SOURCE_MICROPHONE_KEY = "microphone"
        internal const val AUDIO_SOURCE_MEDIA_PROJECTION_KEY = "mediaProjection"
    }
}