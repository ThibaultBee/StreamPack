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

import android.content.Context
import io.github.thibaultbee.streampack.data.BitrateRegulatorConfig
import io.github.thibaultbee.streampack.ext.srt.data.SrtConnectionDescriptor
import io.github.thibaultbee.streampack.ext.srt.internal.endpoints.sinks.SrtSink
import io.github.thibaultbee.streampack.ext.srt.regulator.srt.SrtBitrateRegulator
import io.github.thibaultbee.streampack.ext.srt.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.internal.endpoints.ConnectableCompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.utils.Scheduler
import io.github.thibaultbee.streampack.internal.utils.extensions.defaultTsServiceInfo
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.regulator.IBitrateRegulatorFactory
import io.github.thibaultbee.streampack.streamers.bases.BaseCameraStreamer
import io.github.thibaultbee.streampack.streamers.live.BaseCameraLiveStreamer

/**
 * [BaseCameraStreamer] that sends microphones and camerao frames to a remote Secure Reliable
 * Transport (SRT) device.
 *
 * @param context application context
 * @param tsServiceInfo MPEG-TS service description
 * @param enableAudio [Boolean.true] to capture audio. False to disable audio capture.
 * @param bitrateRegulatorFactory a [IBitrateRegulatorFactory] implementation. Use it to customized bitrate regulator.  If bitrateRegulatorConfig is not null, bitrateRegulatorFactory must not be null.
 * @param bitrateRegulatorConfig bitrate regulator configuration. If bitrateRegulatorFactory is not null, bitrateRegulatorConfig must not be null.
 * @param initialOnErrorListener initialize [OnErrorListener]
 * @param initialOnConnectionListener initialize [OnConnectionListener]
 */
class CameraSrtLiveStreamer(
    context: Context,
    enableAudio: Boolean = true,
    tsServiceInfo: TsServiceInfo = context.defaultTsServiceInfo,
    bitrateRegulatorFactory: IBitrateRegulatorFactory? = null,
    bitrateRegulatorConfig: BitrateRegulatorConfig? = null,
    initialOnErrorListener: OnErrorListener? = null,
    initialOnConnectionListener: OnConnectionListener? = null
) : BaseCameraLiveStreamer(
    context = context,
    enableAudio = enableAudio,
    endpoint = ConnectableCompositeEndpoint(
        TSMuxer().apply { addService(tsServiceInfo) },
        SrtSink()
    ),
    initialOnErrorListener = initialOnErrorListener,
    initialOnConnectionListener = initialOnConnectionListener
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

    private val srtProducer = (endpoint as ConnectableCompositeEndpoint).sink as SrtSink

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
        @Deprecated(
            "Use the new connect(SrtConnectionDescriptor) method",
            replaceWith = ReplaceWith("connect(SrtConnectionDescriptor)")
        )
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
        @Deprecated(
            "Use the new connect(SrtConnectionDescriptor) method",
            replaceWith = ReplaceWith("connect(SrtConnectionDescriptor)")
        )
        set(value) {
            srtProducer.passPhrase = value
        }

    /**
     * Get/set bidirectional latency in milliseconds.
     * **See:** [SRT Socket Options](https://github.com/Haivision/srt/blob/master/docs/API/API-socket-options.md#SRTO_LATENCY)
     */
    override val latency: Int
        /**
         * Get latency in milliseconds
         * @return latency
         */
        get() = srtProducer.latency

    /**
     * Connect to an SRT server with correct Live streaming parameters.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    @Deprecated(
        "Use the new connect(SrtConnectionDescriptor) method",
        replaceWith = ReplaceWith("connect(SrtConnectionDescriptor)")
    )
    override suspend fun connect(ip: String, port: Int) {
        val connection = SrtConnectionDescriptor(ip, port)
        srtProducer.connect(connection)
    }

    /**
     * Connect to an SRT server with correct Live streaming parameters.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param connection the SRT connection
     * @throws Exception if connection has failed or configuration has failed
     */
    override suspend fun connect(connection: SrtConnectionDescriptor) {
        srtProducer.connect(connection)
    }

    /**
     * Same as [BaseCameraLiveStreamer.startStream] but also starts bitrate regulator.
     */
    override suspend fun startStream() {
        super.startStream()
        if (bitrateRegulator != null) {
            scheduler.start()
        }
    }

    /**
     * Same as [BaseCameraLiveStreamer.startStream] but also starts bitrate regulator.
     */
    override suspend fun startStream(url: String) {
        super.startStream(url)
        if (bitrateRegulator != null) {
            scheduler.start()
        }
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
    @Deprecated(
        "Use the new startStream(SrtConnectionDescriptor) method",
        replaceWith = ReplaceWith("startStream(SrtConnectionDescriptor)")
    )
    override suspend fun startStream(ip: String, port: Int) {
        val connection = SrtConnectionDescriptor(ip, port)
        startStream(connection)
    }

    /**
     * Connect to an SRT server and start stream.
     * Same as calling [connect], then [startStream].
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param connection the SRT connection
     * @throws Exception if connection has failed or configuration has failed or [startStream] has failed too.
     */
    override suspend fun startStream(connection: SrtConnectionDescriptor) {
        connect(connection)
        startStream()
    }

    /**
     * Same as [BaseCameraLiveStreamer.stopStream] but also stops bitrate regulator.
     */
    override suspend fun stopStream() {
        scheduler.cancel()
        super.stopStream()
    }
}
