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
package io.github.thibaultbee.streampack.ext.rtmp.services

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import io.github.thibaultbee.streampack.R
import io.github.thibaultbee.streampack.ext.rtmp.streamers.ScreenRecorderRtmpLiveStreamer
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.services.BaseScreenRecorderService

open class ScreenRecorderRtmpLiveService(
    notificationId: Int = DEFAULT_NOTIFICATION_ID,
    channelId: String = DEFAULT_NOTIFICATION_CHANNEL_ID,
    channelNameResourceId: Int = R.string.default_channel_name,
    channelDescriptionResourceId: Int = 0,
) : BaseScreenRecorderService(
    notificationId,
    channelId,
    channelNameResourceId,
    channelDescriptionResourceId
) {
    override fun createStreamer(bundle: Bundle): BaseScreenRecorderStreamer {
        val enableAudio = bundle.getBoolean(ENABLE_AUDIO_KEY)

        return ScreenRecorderRtmpLiveStreamer(
            applicationContext,
            enableAudio = enableAudio,
        )
    }

    companion object {
        /**
         * Starts and binds the service with the appropriate parameters.
         *
         * @param context The application context.
         * @param serviceClass The children service class.
         * @param enableAudio [Boolean.true] to also capture audio. False to disable audio capture.
         * @param onServiceCreated Callback that returns the [ScreenRecorderRtmpLiveStreamer] instance when the service has been connected.
         * @param onServiceDisconnected Callback that will be called when the service is disconnected.
         */
        fun launch(
            context: Context,
            serviceClass: Class<out ScreenRecorderRtmpLiveService>,
            enableAudio: Boolean,
            onServiceCreated: (ScreenRecorderRtmpLiveStreamer) -> Unit,
            onServiceDisconnected: (name: ComponentName?) -> Unit
        ) {
            BaseScreenRecorderService.launch(
                context,
                serviceClass,
                enableAudio,
                { streamer -> onServiceCreated(streamer as ScreenRecorderRtmpLiveStreamer) },
                onServiceDisconnected
            )
        }
    }
}