/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.ext.srt.streamers

import android.Manifest
import android.app.Service
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.SrtProducer
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.logger.StreamPackLogger
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.DefaultSrtBitrateRegulatorFactory
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.SrtBitrateRegulator
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IAdaptiveLiveStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import java.net.SocketException

/**
 * [BaseScreenRecorderStreamer] that sends audio/video frames to a remote device with Secure Reliable
 * Transport (SRT) Protocol.
 * To run this streamer while application is on background, you will have to create a [Service].
 * As an example, check for `screenrecorder`.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to also capture audio. False to disable audio capture.
 * @param bitrateRegulatorFactory a [IBitrateRegulatorFactory] implementation. Use it to customized bitrate regulator.  If bitrateRegulatorConfig is not null, bitrateRegulatorFactory must not be null.
 * @param bitrateRegulatorConfig bitrate regulator configuration. If bitrateRegulatorFactory is not null, bitrateRegulatorConfig must not be null.
 */
class ScreenRecorderSrtLiveStreamer(
    context: Context,
    tsServiceInfo: TsServiceInfo,
    logger: ILogger,
    enableAudio: Boolean,
    bitrateRegulatorFactory: IBitrateRegulatorFactory?,
    bitrateRegulatorConfig: BitrateRegulatorConfig?,
) : BaseScreenRecorderStreamer(
    context = context,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    endpoint = SrtProducer(logger = logger),
    logger = logger,
    enableAudio = enableAudio
),
    ISrtLiveStreamer {

    /**
     * Bitrate regulator. Calls regularly by [scheduler]. Don't call it otherwise or you might break regulation.
     */
    private val bitrateRegulator = bitrateRegulatorConfig?.let { config ->
        bitrateRegulatorFactory?.newBitrateRegulator(
            config,
            { settings.video.bitrate = it },
            { settings.audio.bitrate = it }
        ) as SrtBitrateRegulator
    }

    /**
     * Scheduler for bitrate regulation
     */
    private val scheduler = Scheduler(500) {
        bitrateRegulator?.update(srtProducer.stats, settings.video.bitrate, settings.audio.bitrate)
            ?: throw UnsupportedOperationException("Scheduler runs but no bitrate regulator set")
    }

    /**
     * Listener to manage SRT connection.
     */
    override var onConnectionListener: OnConnectionListener? = null
        set(value) {
            srtProducer.onConnectionListener = value
            field = value
        }

    private val srtProducer = endpoint as SrtProducer

    /**
     * Get/set SRT stream ID.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#srto_streamid)
     */
    override var streamId: String
        /**
         * Get SRT stream ID
         * @return stream ID
         */
        get() = srtProducer.streamId
        /**
         * @param value stream ID
         */
        set(value) {
            srtProducer.streamId = value
        }

    /**
     * Get/set SRT passphrase.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#srto_passphrase)
     */
    override var passPhrase: String
        /**
         * Get SRT passphrase
         * @return passphrase
         */
        get() = srtProducer.passPhrase
        /**
         * @param value passphrase
         */
        set(value) {
            srtProducer.passPhrase = value
        }

    /**
     * Connect to an SRT server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(ip: String, port: Int) {
        srtProducer.connect(ip, port)
    }

    /**
     * Disconnect from the connected SRT server.
     *
     * @throws SocketException is not connected
     */
    override fun disconnect() {
        srtProducer.disconnect()
    }

    /**
     * Same as [BaseScreenRecorderStreamer.startStream] but also starts bitrate regulator.
     */
    override fun startStream() {
        if (bitrateRegulator != null) {
            scheduler.start()
        }
        super.startStream()
    }

    /**
     * Connect to an SRT server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun startStream(ip: String, port: Int) {
        connect(ip, port)
        startStream()
    }

    /**
     * Same as [BaseScreenRecorderStreamer.stopStream] but also stops bitrate regulator.
     */
    override fun stopStream() {
        scheduler.cancel()
        super.stopStream()
    }

    /**
     * Builder class for [ScreenRecorderSrtLiveStreamer] objects. Use this class to configure and create an [ScreenRecorderSrtLiveStreamer] instance.
     */
    data class Builder(
        private var logger: ILogger = StreamPackLogger(),
        private var audioConfig: AudioConfig? = null,
        private var videoConfig: VideoConfig? = null,
        private var enableAudio: Boolean = true,
        private var streamId: String? = null,
        private var passPhrase: String? = null,
        private var bitrateRegulatorFactory: IBitrateRegulatorFactory? = null,
        private var bitrateRegulatorConfig: BitrateRegulatorConfig? = null
    ) : IStreamerBuilder, ITsStreamerBuilder, IAdaptiveLiveStreamerBuilder {
        private lateinit var context: Context
        private lateinit var tsServiceInfo: TsServiceInfo

        /**
         * Set application context. It is mandatory to set context.
         *
         * @param context application context.
         */
        override fun setContext(context: Context) = apply { this.context = context }

        /**
         * Set TS service info. It is mandatory to set TS service info.
         *
         * @param tsServiceInfo TS service info.
         */
        override fun setServiceInfo(tsServiceInfo: TsServiceInfo) =
            apply { this.tsServiceInfo = tsServiceInfo }

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
         * Set SRT stream id.
         *
         * @param streamId string describing SRT stream id
         */
        override fun setStreamId(streamId: String) = apply {
            this.streamId = streamId
        }

        /**
         * Set SRT passphrase.
         *
         * @param passPhrase string describing SRT pass phrase
         */
        override fun setPassPhrase(passPhrase: String) = apply {
            this.passPhrase = passPhrase
        }

        /**
         * Set SRT bitrate regulator class and configuration.
         *
         * @param bitrateRegulatorFactory bitrate regulator factory. If you don't want to implement your own bitrate regulator, use [DefaultSrtBitrateRegulatorFactory]
         * @param bitrateRegulatorConfig bitrate regulator configuration.
         */
        override fun setBitrateRegulator(
            bitrateRegulatorFactory: IBitrateRegulatorFactory?,
            bitrateRegulatorConfig: BitrateRegulatorConfig?
        ) = apply {
            this.bitrateRegulatorFactory = bitrateRegulatorFactory
            this.bitrateRegulatorConfig = bitrateRegulatorConfig
        }

        /**
         * Combines all of the characteristics that have been set and return a new [ScreenRecorderSrtLiveStreamer] object.
         *
         * @return a new [ScreenRecorderSrtLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO])
        override fun build(): ScreenRecorderSrtLiveStreamer {
            return ScreenRecorderSrtLiveStreamer(
                context,
                tsServiceInfo,
                logger,
                enableAudio,
                bitrateRegulatorFactory,
                bitrateRegulatorConfig
            ).also { streamer ->
                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }

                streamId?.let {
                    streamer.streamId = it
                }

                passPhrase?.let {
                    streamer.passPhrase = it
                }
            }
        }
    }
}