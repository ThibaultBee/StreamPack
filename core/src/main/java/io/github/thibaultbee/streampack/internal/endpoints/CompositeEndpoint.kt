/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.streampack.internal.endpoints

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxerInfo
import io.github.thibaultbee.streampack.internal.endpoints.sinks.ISink

/**
 * An [IEndpoint] implementation that combines a [IMuxer] and a [ISink].
 */
open class CompositeEndpoint(val muxer: IMuxer, open val sink: ISink) : IEndpoint {
    /**
     * The total start bitrate of all streams.
     * It is used to configure the sink.
     */
    private var bitrate = 0

    override val info = CompositeEndpointInfo(muxer.info)

    init {
        muxer.listener = object : IMuxer.IMuxerListener {
            override fun onOutputFrame(packet: Packet) {
                sink.write(packet)
            }
        }
    }

    override fun write(frame: Frame, streamPid: Int) = muxer.write(frame, streamPid)

    override fun addStreams(streamsConfig: List<Config>): Map<Config, Int> {
        val streamIds = muxer.addStreams(streamsConfig)
        bitrate += streamsConfig.sumOf { it.startBitrate }
        return streamIds
    }

    override fun addStream(streamConfig: Config): Int {
        val streamId = muxer.addStream(streamConfig)
        bitrate += streamConfig.startBitrate
        return streamId
    }

    override suspend fun startStream() {
        sink.configure(bitrate)
        sink.startStream()
        muxer.startStream()
    }

    /**
     * Stops the stream and releases the sink.
     *
     * It also clears registered streams and resets the bitrate.
     */
    override suspend fun stopStream() {
        muxer.stopStream()
        sink.stopStream()
        bitrate = 0
    }

    override fun release() {
        sink.release()
    }

    class CompositeEndpointInfo(
        val muxerInfo: IMuxerInfo
    ) : IEndpointSettings.IEndpointInfo {
        override val audio = object : IEndpointSettings.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = muxerInfo.audio.supportedEncoders
            override val supportedSampleRates = muxerInfo.audio.supportedSampleRates
            override val supportedByteFormats = muxerInfo.audio.supportedByteFormats
        }

        override val video = object : IEndpointSettings.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = muxerInfo.video.supportedEncoders
        }
    }
}