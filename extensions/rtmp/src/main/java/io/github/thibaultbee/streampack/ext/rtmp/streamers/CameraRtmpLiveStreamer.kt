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
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IRtmpLiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerPreviewBuilder
import io.github.thibaultbee.streampack.streamers.live.BaseCameraLiveStreamer
import java.net.SocketException

/**
 * A [BaseCameraLiveStreamer] that sends microphone and camera frames to a remote RTMP device.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 */
class CameraRtmpLiveStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
) : BaseCameraLiveStreamer(
    context = context,
    logger = logger,
    enableAudio = enableAudio,
    endpoint = RtmpProducer(logger = logger),
    muxer = FlvMuxer(context = context, writeToFile = false)
),
    IRtmpLiveStreamer {

    private val rtmpProducer = endpoint as RtmpProducer

    /**
     * Connect to a RTMP server with correct Live streaming parameters.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url (syntax: rtmp://server/streamKey)
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(url: String) {
        rtmpProducer.connect(url)
    }

    /**
     * Disconnect from the connected RTMP server.
     *
     * @throws SocketException is not connected
     */
    override fun disconnect() {
        rtmpProducer.disconnect()
    }

    /**
     * Connect to a RTMP server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url (syntax: rtmp://server/streamKey)
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    override suspend fun startStream(url: String) {
        connect(url)
        startStream()
    }

    /**
     * Builder class for [CameraRtmpLiveStreamer] objects. Use this class to configure and create an [CameraRtmpLiveStreamer] instance.
     */
     class Builder : BaseCameraLiveStreamer.Builder() {
        /**
         * Combines all of the characteristics that have been set and return a new
         * [CameraRtmpLiveStreamer] object.
         *
         * @return a new [CameraRtmpLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): CameraRtmpLiveStreamer {
            return CameraRtmpLiveStreamer(
                context,
                logger,
                enableAudio,
            ).also { streamer ->
                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }

                previewSurface?.let {
                    streamer.startPreview(it)
                }
            }
        }
    }
}
