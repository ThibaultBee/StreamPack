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
package com.github.thibaultbee.streampack.streamers.rtmp

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.internal.endpoints.RtmpProducer
import com.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import com.github.thibaultbee.streampack.listeners.OnConnectionListener
import com.github.thibaultbee.streampack.logger.ILogger
import com.github.thibaultbee.streampack.logger.StreamPackLogger
import com.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.IRtmpLiveStreamer
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import com.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerPreviewBuilder
import java.net.SocketException

/**
 * [BaseCameraStreamer] that sends audio/video frames to a remote device with RTMP Protocol.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 */
class CameraRtmpLiveStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
) : BaseCameraStreamer(
    context = context,
    endpoint = RtmpProducer(logger = logger),
    muxer = FlvMuxer(context = context, writeToFile = false),
    logger = logger,
    enableAudio = enableAudio
),
    IRtmpLiveStreamer {

    /**
     * Listener to manage RTMP connection.
     */
    override var onConnectionListener: OnConnectionListener? = null
        set(value) {
            rtmpProducer.onConnectionListener = value
            field = value
        }

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
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var videoConfig: VideoConfig? = null,
        private var previewSurface: Surface? = null,
        private var enableAudio: Boolean = true,
    ) : IStreamerBuilder, IStreamerPreviewBuilder {
        private lateinit var context: Context

        /**
         * Set application context. It is mandatory to set context.
         *
         * @param context application context.
         */
        override fun setContext(context: Context) = apply { this.context = context }

        /**
         * Set logger.
         *
         * @param logger [ILogger] implementation
         */
        override fun setLogger(logger: ILogger) = apply { this.logger = logger }

        /**
         * Set both audio and video configuration.
         * Configurations can be change later with [configure].
         * Same as calling both [setAudioConfiguration] and [setVideoConfiguration].
         *
         * @param audioConfig audio configuration
         * @param videoConfig video configuration
         */
        override fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig) = apply {
            this.audioConfig = audioConfig
            this.videoConfig = videoConfig
        }

        /**
         * Set audio configurations.
         * Configurations can be change later with [configure].
         *
         * @param audioConfig audio configuration
         */
        override fun setAudioConfiguration(audioConfig: AudioConfig) = apply {
            this.audioConfig = audioConfig
        }

        /**
         * Set video configurations.
         * Configurations can be change later with [configure].
         *
         * @param videoConfig video configuration
         */
        override fun setVideoConfiguration(videoConfig: VideoConfig) = apply {
            this.videoConfig = videoConfig
        }

        /**
         * Disable audio.
         * Audio is enabled by default.
         * When audio is disabled, there is no way to enable it again.
         */
        override fun disableAudio() = apply {
            this.enableAudio = false
        }

        /**
         * Set preview surface.
         * If provided, it starts preview.
         *
         * @param previewSurface surface where to display preview
         */
        override fun setPreviewSurface(previewSurface: Surface) = apply {
            this.previewSurface = previewSurface
        }

        /**
         * Combines all of the characteristics that have been set and return a new [CameraRtmpLiveStreamer] object.
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
