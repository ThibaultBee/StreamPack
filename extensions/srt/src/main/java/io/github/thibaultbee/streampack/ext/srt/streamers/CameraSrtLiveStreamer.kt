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
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.SrtProducer
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.DefaultSrtBitrateRegulatorFactory
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.SrtBitrateRegulator
import io.github.thibaultbee.streampack.internal.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.logger.ILogger
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.builders.IAdaptiveLiveStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.ISrtLiveStreamerBuilder
import io.github.thibaultbee.streampack.streamers.interfaces.builders.ITsStreamerBuilder
import io.github.thibaultbee.streampack.streamers.live.BaseCameraLiveStreamer

/**
 * [BaseCameraStreamer] that sends microphones and camerao frames to a remote Secure Reliable
 * Transport (SRT) device.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param logger a [ILogger] implementation
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param bitrateRegulatorFactory a [IBitrateRegulatorFactory] implementation. Use it to customized bitrate regulator.  If bitrateRegulatorConfig is not null, bitrateRegulatorFactory must not be null.
 * @param bitrateRegulatorConfig bitrate regulator configuration. If bitrateRegulatorFactory is not null, bitrateRegulatorConfig must not be null.
 */
class CameraSrtLiveStreamer(
    context: Context,
    logger: ILogger,
    enableAudio: Boolean,
    tsServiceInfo: TsServiceInfo,
    bitrateRegulatorFactory: IBitrateRegulatorFactory?,
    bitrateRegulatorConfig: BitrateRegulatorConfig?
) : BaseCameraLiveStreamer(
    context = context,
    logger = logger,
    enableAudio = enableAudio,
    muxer = TSMuxer().apply { addService(tsServiceInfo) },
    endpoint = SrtProducer(logger = logger)
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
     * Connect to an SRT server with correct Live streaming parameters.
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
     * Same as [BaseCameraStreamer.startStream] but also starts bitrate regulator.
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
    @RequiresPermission(allOf = [Manifest.permission.CAMERA])
    override suspend fun startStream(ip: String, port: Int) {
        connect(ip, port)
        startStream()
    }

    /**
     * Same as [BaseCameraLiveStreamer.stopStream] but also stops bitrate regulator.
     */
    override fun stopStream() {
        scheduler.cancel()
        super.stopStream()
    }

    /**
     * Builder class for [CameraSrtLiveStreamer] objects. Use this class to configure and create
     * an [CameraSrtLiveStreamer] instance.
     */
    class Builder : BaseCameraLiveStreamer.Builder(), ITsStreamerBuilder, ISrtLiveStreamerBuilder,
        IAdaptiveLiveStreamerBuilder {
        private lateinit var tsServiceInfo: TsServiceInfo
        private var bitrateRegulatorFactory: IBitrateRegulatorFactory? = null
        private var bitrateRegulatorConfig: BitrateRegulatorConfig? = null
        private var streamId: String? = null
        private var passPhrase: String? = null

        /**
         * Set TS service info. It is mandatory to set TS service info.
         *
         * @param tsServiceInfo TS service info.
         */
        override fun setServiceInfo(tsServiceInfo: TsServiceInfo) =
            apply { this.tsServiceInfo = tsServiceInfo }

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
         * Combines all of the characteristics that have been set and return a new
         * [CameraSrtLiveStreamer] object.
         *
         * @return a new [CameraSrtLiveStreamer] object
         */
        @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
        override fun build(): CameraSrtLiveStreamer {
            return CameraSrtLiveStreamer(
                context,
                logger,
                enableAudio,
                tsServiceInfo,
                bitrateRegulatorFactory,
                bitrateRegulatorConfig
            ).also { streamer ->
                if (videoConfig != null) {
                    streamer.configure(audioConfig, videoConfig!!)
                }

                previewSurface?.let {
                    streamer.startPreview(it)
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
