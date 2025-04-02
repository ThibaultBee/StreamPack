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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.data.Packet
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxerInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ISinkInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.SinkConfiguration
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * An [IEndpointInternal] implementation that combines a [IMuxerInternal] and a [ISinkInternal].
 */
class CompositeEndpoint(
    override val muxer: IMuxerInternal,
    override val sink: ISinkInternal
) :
    ICompositeEndpointInternal {
    /**
     * The video and audio configurations.
     * It is used to configure the sink.
     */
    private val configurations = mutableListOf<CodecConfig>()

    override val info by lazy { EndpointInfo(muxer.info) }
    override fun getInfo(type: MediaDescriptor.Type) = info

    override val metrics: Any
        get() = sink.metrics

    init {
        muxer.listener = object :
            IMuxerInternal.IMuxerListener {
            override fun onOutputFrame(packet: Packet) {
                runBlocking {
                    sink.write(packet)
                }
            }
        }
    }

    override val isOpenFlow: StateFlow<Boolean>
        get() = sink.isOpenFlow

    override suspend fun open(descriptor: MediaDescriptor) {
        sink.open(descriptor)
    }

    override suspend fun close() {
        sink.close()
    }

    override suspend fun write(
        frame: Frame,
        streamPid: Int
    ) = muxer.write(frame, streamPid)

    override fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        val streamIds = muxer.addStreams(streamConfigs)
        configurations.addAll(streamConfigs)
        return streamIds
    }

    override fun addStream(streamConfig: CodecConfig): Int {
        val streamId = muxer.addStream(streamConfig)
        configurations.add(streamConfig)
        return streamId
    }

    override suspend fun startStream() {
        sink.configure(SinkConfiguration(configurations))
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

    class EndpointInfo(
        val muxerInfo: IMuxer.IMuxerInfo
    ) : IEndpoint.IEndpointInfo {
        override val audio by lazy {
            object : IEndpoint.IEndpointInfo.IAudioEndpointInfo {
                override val supportedEncoders by lazy { muxerInfo.audio.supportedEncoders }
                override val supportedSampleRates by lazy { muxerInfo.audio.supportedSampleRates }
                override val supportedByteFormats by lazy { muxerInfo.audio.supportedByteFormats }
            }
        }

        override val video by lazy {
            object : IEndpoint.IEndpointInfo.IVideoEndpointInfo {
                override val supportedEncoders by lazy { muxerInfo.video.supportedEncoders }
            }
        }
    }
}

/**
 * A factory to build a [CompositeEndpoint].
 */
class CompositeEndpointFactory(
    val muxer: IMuxerInternal,
    val sink: ISinkInternal
) : IEndpointInternal.Factory {
    override fun create(context: Context): IEndpointInternal {
        return CompositeEndpoint(muxer, sink)
    }
}
