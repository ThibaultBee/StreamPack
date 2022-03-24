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
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.rtmp.internal.endpoints.RtmpProducer
import io.github.thibaultbee.streampack.internal.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.internal.sources.AudioCapture
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.streamers.bases.BaseStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IRtmpLiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import java.net.SocketException

/**
 * [BaseStreamer] that sends only audio frames to a remote device with RTMP Protocol.
 *
 * @param context application context
 * @param logger a [ILogger] implementation
 */
class AudioOnlyRtmpLiveStreamer(
    context: Context,
    logger: ILogger
) : BaseStreamer(
    context = context,
    videoCapture = null,
    audioCapture = AudioCapture(logger),
    muxer = FlvMuxer(context = context, writeToFile = false),
    endpoint = RtmpProducer(logger = logger),
    logger = logger
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
     * Connect to an RTMP server with correct Live streaming parameters.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
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
     * Connect to an RTMP server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
    override suspend fun startStream(url: String) {
        connect(url)
        startStream()
    }

    /**
     * Builder class for [AudioOnlyRtmpLiveStreamer] objects. Use this class to configure and create an [AudioOnlyRtmpLiveStreamer] instance.
     */
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var streamId: String? = null,
        private var passPhrase: String? = null
    ) : IStreamerBuilder {
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
         * Set audio configuration.
         * Configurations can be change later with [configure].
         * Video configuration is not used.
         *
         * @param audioConfig audio configuration
         * @param videoConfig video configuration. Not used.
         */
        override fun setConfiguration(audioConfig: AudioConfig, videoConfig: VideoConfig) = apply {
            this.audioConfig = audioConfig
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
         * Set video configurations. Do not use.
         *
         * @param videoConfig video configuration
         */
        override fun setVideoConfiguration(videoConfig: VideoConfig) = apply {
            throw UnsupportedOperationException("Do not set video configuration on audio only streamer")
        }

        /**
         * Disable audio. Do not use.
         */
        override fun disableAudio(): IStreamerBuilder {
            throw UnsupportedOperationException("Do not disable audio on audio only streamer")
        }

        /**
         * Combines all of the characteristics that have been set and return a new [AudioOnlyRtmpLiveStreamer] object.
         *
         * @return a new [AudioOnlyRtmpLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
        override fun build(): AudioOnlyRtmpLiveStreamer {
            return AudioOnlyRtmpLiveStreamer(
                context,
                logger
            ).also { streamer ->
                streamer.configure(audioConfig)
            }
        }
    }
}
