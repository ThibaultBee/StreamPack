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

import android.app.Service
import android.content.Context
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.video.ExtendedVideoTag
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.live.BaseScreenRecorderLiveStreamer

/**
 * A [BaseScreenRecorderStreamer] that sends microphone and screen frames to a remote RTMP device.
 * To run this streamer while application is on background, you will have to create a [Service].
 * As an example, check for `screenrecorder` application.
 *
 * @param context application context
 * @param enableAudio [Boolean.true] to also capture audio. False to disable audio capture.
 * @param initialOnErrorListener initialize [OnErrorListener]
 * @param initialOnConnectionListener initialize [OnConnectionListener]
 */
class ScreenRecorderRtmpLiveStreamer(
    context: Context,
    enableAudio: Boolean = true,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : BaseScreenRecorderLiveStreamer(
    context = context,
    enableAudio = enableAudio,
    muxer = FlvMuxer(context = context, writeToFile = false),
    endpoint = RtmpProducer(hasAudio = enableAudio, hasVideo = true),
    initialOnErrorListener = initialOnErrorListener,
    initialOnConnectionListener = initialOnConnectionListener
) {
    private val rtmpProducer = endpoint as RtmpProducer
    override suspend fun connect(url: String) {
        require(videoConfig != null) {
            "Video config must be set before connecting to send the video codec in the connect message"
        }
        val codecMimeType = videoConfig!!.mimeType
        if (ExtendedVideoTag.isSupportedCodec(codecMimeType)) {
            rtmpProducer.supportedVideoCodecs = listOf(codecMimeType)
        }
        rtmpProducer.supportedVideoCodecs = listOf(videoConfig!!.mimeType)
        super.connect(url)
    }
}