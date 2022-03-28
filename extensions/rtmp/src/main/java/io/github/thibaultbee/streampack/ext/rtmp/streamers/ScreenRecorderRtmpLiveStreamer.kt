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
package io.github.thibaultbee.streampack.ext.rtmp.streamers

import android.Manifest
import android.app.Service
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IRtmpLiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import io.github.thibaultbee.streampack.streamers.live.BaseScreenRecorderLiveStreamer

/**
 * A [BaseScreenRecorderStreamer] that sends microphone and screen frames to a remote RTMP device.
 * To run this streamer while application is on background, you will have to create a [Service].
 * As an example, check for `screenrecorder` application.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to also capture audio. False to disable audio capture.
 */
class ScreenRecorderRtmpLiveStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean
) : BaseScreenRecorderLiveStreamer(
    context = context,
    logger = logger,
    enableAudio = enableAudio,
    muxer = FlvMuxer(context = context, writeToFile = false),
    endpoint = RtmpProducer(logger = logger)
),
    IRtmpLiveStreamer {
    private val rtmpProducer = endpoint as RtmpProducer

    /**
     * Connect to an RTMP server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(url: String) {
        rtmpProducer.connect(url)
    }

    /**
     * Connect to an RTMP server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun startStream(url: String) {
        connect(url)
        startStream()
    }

    /**
     * Builder class for [ScreenRecorderRtmpLiveStreamer] objects. Use this class to configure and
     * create an [ScreenRecorderRtmpLiveStreamer] instance.
     */
    class Builder : BaseScreenRecorderLiveStreamer.Builder() {
        /**
         * Combines all of the characteristics that have been set and return a new
         * [ScreenRecorderRtmpLiveStreamer] object.
         *
         * @return a new [ScreenRecorderRtmpLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
        override fun build(): ScreenRecorderRtmpLiveStreamer {
            return ScreenRecorderRtmpLiveStreamer(
                context,
                logger,
                enableAudio
            ).also { streamer ->
                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }
            }
        }
    }
}