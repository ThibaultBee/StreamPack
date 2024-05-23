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
package io.github.thibaultbee.streampack.internal.endpoints.composites

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.endpoints.IEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.IPublicEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.IPublicMuxer
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.EndpointConfiguration
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.ISink
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * An [IEndpoint] implementation that combines a [IMuxer] and a [ISink].
 */
open class CompositeEndpoint(final override val muxer: IMuxer, override val sink: ISink) :
    ICompositeEndpoint {
    /**
     * The video and audio configurations.
     * It is used to configure the sink.
     */
    private val configurations = mutableListOf<Config>()

    override val info = EndpointInfo(muxer.info)
    override fun getInfo(type: MediaDescriptor.Type) = info

    override val metrics: Any
        get() = sink.metrics

    init {
        muxer.listener = object :
            IMuxer.IMuxerListener {
            override fun onOutputFrame(packet: Packet) {
                runBlocking {
                    sink.write(packet)
                }
            }
        }
    }

    override val isOpened: StateFlow<Boolean>
        get() = sink.isOpened

    override suspend fun open(descriptor: MediaDescriptor) {
        sink.open(descriptor)
    }

    override suspend fun close() {
        stopStream()
        sink.close()
    }

    override suspend fun write(frame: Frame, streamPid: Int) = muxer.write(frame, streamPid)

    override fun addStreams(streamConfigs: List<Config>): Map<Config, Int> {
        val streamIds = muxer.addStreams(streamConfigs)
        configurations.addAll(streamConfigs)
        return streamIds
    }

    override fun addStream(streamConfig: Config): Int {
        val streamId = muxer.addStream(streamConfig)
        configurations.add(streamConfig)
        return streamId
    }

    override suspend fun startStream() {
        sink.configure(EndpointConfiguration(configurations))
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
        configurations.clear()
    }

    override fun release() {
        runBlocking {
            stopStream()
            close()
        }
    }

    class EndpointInfo(
        val muxerInfo: IPublicMuxer.IMuxerInfo
    ) : IPublicEndpoint.IEndpointInfo {
        override val audio = object : IPublicEndpoint.IEndpointInfo.IAudioEndpointInfo {
            override val supportedEncoders = muxerInfo.audio.supportedEncoders
            override val supportedSampleRates = muxerInfo.audio.supportedSampleRates
            override val supportedByteFormats = muxerInfo.audio.supportedByteFormats
        }

        override val video = object : IPublicEndpoint.IEndpointInfo.IVideoEndpointInfo {
            override val supportedEncoders = muxerInfo.video.supportedEncoders
        }
    }
}
