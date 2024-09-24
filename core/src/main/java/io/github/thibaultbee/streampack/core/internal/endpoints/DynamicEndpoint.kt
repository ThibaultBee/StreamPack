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
package io.github.thibaultbee.streampack.core.internal.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.data.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.CompositeEndpoints
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.internal.utils.DerivedStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [IEndpoint].
 *
 * It creates an [IEndpoint] based on the [MediaDescriptor].
 */
class DynamicEndpoint(
    private val context: Context
) : IEndpoint {
    // Current endpoint
    private var _endpointFlow: MutableStateFlow<IEndpoint?> = MutableStateFlow(null)
    private val endpointFlow: StateFlow<IEndpoint?> = _endpointFlow

    private val _endpoint: IEndpoint?
        get() = endpointFlow.value

    private val endpoint: IEndpoint
        get() {
            val endpoint = _endpoint
            require(endpoint != null) { "Endpoint is not open" }
            return endpoint
        }

    // Endpoints
    private var mediaMuxerEndpoint: IEndpoint? = null
    private var flvFileEndpoint: IEndpoint? = null
    private var flvContentEndpoint: IEndpoint? = null
    private var tsFileEndpoint: IEndpoint? = null
    private var tsContentEndpoint: IEndpoint? = null
    private var srtEndpoint: IEndpoint? = null
    private var rtmpEndpoint: IEndpoint? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isOpened: StateFlow<Boolean> = DerivedStateFlow(
        getValue = { _endpoint?.isOpened?.value ?: false },
        flow = endpointFlow.flatMapLatest { it?.isOpened ?: MutableStateFlow(false) }
    )

    /**
     * Only available when the endpoint is opened.
     */
    override val info: IPublicEndpoint.IEndpointInfo
        get() = endpoint.info

    override fun getInfo(type: MediaDescriptor.Type) = getEndpoint(type).getInfo(type)

    override val metrics: Any
        get() = endpoint.metrics

    override suspend fun open(descriptor: MediaDescriptor) {
        require(!isOpened.value) { "Endpoint is already opened" }

        _endpointFlow.update {
            getEndpointAndConfig(descriptor).apply {
                open(descriptor)
            }
        }
    }

    override fun addStreams(streamConfigs: List<Config>) =
        endpoint.addStreams(streamConfigs)

    override fun addStream(streamConfig: Config) = endpoint.addStream(streamConfig)

    override suspend fun write(frame: Frame, streamPid: Int) = endpoint.write(frame, streamPid)

    override suspend fun startStream() = endpoint.startStream()

    override suspend fun stopStream() {
        _endpoint?.stopStream()
    }

    override suspend fun close() {
        try {
            _endpoint?.close()
        } finally {
            _endpointFlow.update {
                null
            }
        }
    }

    private fun getEndpointAndConfig(mediaDescriptor: MediaDescriptor): IEndpoint {
        val endpoint = getEndpoint(mediaDescriptor.type)

        if (endpoint is CompositeEndpoint) {
            if (endpoint.muxer is TSMuxer) {
                // Clean up services
                endpoint.muxer.removeServices()
                val serviceInfo = mediaDescriptor.getCustomData(TSServiceInfo::class.java)
                    ?: createDefaultTsServiceInfo()
                endpoint.muxer.addService(serviceInfo)
            }
        }

        return endpoint
    }

    private fun getEndpoint(type: MediaDescriptor.Type): IEndpoint {
        return when (type.sinkType) {
            MediaSinkType.FILE -> when (type.containerType) {
                MediaContainerType.MP4, MediaContainerType.WEBM, MediaContainerType.OGG, MediaContainerType.THREEGP -> getMediaMuxerEndpoint()
                MediaContainerType.TS -> getTsFileEndpoint()
                MediaContainerType.FLV -> getFlvFileEndpoint()
            }

            MediaSinkType.CONTENT -> when (type.containerType) {
                MediaContainerType.MP4, MediaContainerType.WEBM, MediaContainerType.OGG, MediaContainerType.THREEGP -> getMediaMuxerEndpoint()
                MediaContainerType.TS -> getTsContentEndpoint()
                MediaContainerType.FLV -> getFlvContentEndpoint()
            }

            MediaSinkType.SRT -> getSrtEndpoint()

            MediaSinkType.RTMP -> getRtmpEndpoint()
        }
    }

    private fun getMediaMuxerEndpoint(): IEndpoint {
        if (mediaMuxerEndpoint == null) {
            mediaMuxerEndpoint = MediaMuxerEndpoint(context)
        }
        return mediaMuxerEndpoint!!
    }

    private fun getFlvFileEndpoint(): IEndpoint {
        if (flvFileEndpoint == null) {
            flvFileEndpoint = CompositeEndpoint(
                FlvMuxer(
                    isForFile = true
                ), FileSink()
            )
        }
        return flvFileEndpoint!!
    }

    private fun getFlvContentEndpoint(): IEndpoint {
        if (flvContentEndpoint == null) {
            flvContentEndpoint = CompositeEndpoint(
                FlvMuxer(
                    isForFile = true
                ), ContentSink(context)
            )
        }
        return flvContentEndpoint!!
    }

    private fun getTsFileEndpoint(): IEndpoint {
        if (tsFileEndpoint == null) {
            tsFileEndpoint = CompositeEndpoint(
                TSMuxer(),
                FileSink()
            )
        }
        return tsFileEndpoint!!
    }

    private fun getTsContentEndpoint(): IEndpoint {
        if (tsContentEndpoint == null) {
            tsContentEndpoint = CompositeEndpoint(
                TSMuxer(), ContentSink(context)
            )
        }
        return tsContentEndpoint!!
    }

    private fun getSrtEndpoint(): IEndpoint {
        if (srtEndpoint == null) {
            srtEndpoint = CompositeEndpoints.createSrtEndpoint(null)
        }
        return srtEndpoint!!
    }

    private fun getRtmpEndpoint(): IEndpoint {
        if (rtmpEndpoint == null) {
            rtmpEndpoint = CompositeEndpoints.createRtmpEndpoint()
        }
        return rtmpEndpoint!!
    }
}
