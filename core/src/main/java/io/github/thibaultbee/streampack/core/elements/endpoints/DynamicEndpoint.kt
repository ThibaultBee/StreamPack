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
package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoints
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TsMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.elements.utils.DerivedStateFlow
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.outputs.encoding.EncodingPipelineOutputDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * An implementation of [IEndpointInternal] where the endpoint is created based on the [MediaDescriptor].
 *
 * @param context The application context
 */
open class DynamicEndpoint(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher
) : IEndpointInternal {
    // Current endpoint
    private var _endpointFlow: MutableStateFlow<IEndpointInternal?> = MutableStateFlow(null)
    private val endpointFlow = _endpointFlow.asStateFlow()

    private val _endpoint: IEndpointInternal?
        get() = endpointFlow.value

    private val endpoint: IEndpointInternal
        get() = requireNotNull(_endpoint) { "Endpoint is not open" }

    // Endpoints
    private var mediaMuxerEndpoint: IEndpointInternal? = null
    private var flvFileEndpoint: IEndpointInternal? = null
    private var flvContentEndpoint: IEndpointInternal? = null
    private var tsFileEndpoint: IEndpointInternal? = null
    private var tsContentEndpoint: IEndpointInternal? = null
    private var srtEndpoint: IEndpointInternal? = null
    private var rtmpEndpoint: IEndpointInternal? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isOpenFlow: StateFlow<Boolean> = DerivedStateFlow(
        getValue = { _endpoint?.isOpenFlow?.value ?: false },
        flow = endpointFlow.flatMapLatest { it?.isOpenFlow ?: MutableStateFlow(false) }
    )

    /**
     * Only available when the endpoint is opened.
     */
    override val info: IEndpoint.IEndpointInfo
        get() = endpoint.info

    override fun getInfo(type: MediaDescriptor.Type) = getEndpoint(type).getInfo(type)

    override val metrics: Any
        get() = endpoint.metrics

    override suspend fun open(descriptor: MediaDescriptor) {
        if (isOpenFlow.value) {
            Logger.w(TAG, "DynamicEndpoint is already opened")
            return
        }

        _endpointFlow.emit(getEndpointAndConfig(descriptor))
        _endpoint?.open(descriptor) ?: throw IllegalStateException("Endpoint is null")
    }

    override fun addStreams(streamConfigs: List<CodecConfig>) =
        endpoint.addStreams(streamConfigs)

    override fun addStream(streamConfig: CodecConfig) = endpoint.addStream(streamConfig)

    override suspend fun write(frame: Frame, streamPid: Int) = endpoint.write(frame, streamPid)

    override suspend fun startStream() = endpoint.startStream()

    override suspend fun stopStream() {
        _endpoint?.stopStream()
    }

    override suspend fun close() {
        try {
            _endpoint?.close()
        } finally {
            _endpointFlow.emit(null)
        }
    }

    private fun getEndpointAndConfig(mediaDescriptor: MediaDescriptor): IEndpointInternal {
        val endpoint = getEndpoint(mediaDescriptor.type)

        if (endpoint is CompositeEndpoint) {
            if (endpoint.muxer is TsMuxer) {
                // Clean up services
                endpoint.muxer.removeServices()
                val serviceInfo = mediaDescriptor.getCustomData(TSServiceInfo::class.java)
                    ?: createDefaultTsServiceInfo()
                endpoint.muxer.addService(serviceInfo)
            }
        }

        return endpoint
    }

    private fun getEndpoint(type: MediaDescriptor.Type): IEndpointInternal {
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

    private fun getMediaMuxerEndpoint(): IEndpointInternal {
        if (mediaMuxerEndpoint == null) {
            mediaMuxerEndpoint = MediaMuxerEndpoint(context, ioDispatcher)
        }
        return mediaMuxerEndpoint!!
    }

    private fun getFlvFileEndpoint(): IEndpointInternal {
        if (flvFileEndpoint == null) {
            flvFileEndpoint = CompositeEndpoint(
                FlvMuxer(
                    isForFile = true
                ), FileSink()
            )
        }
        return flvFileEndpoint!!
    }

    private fun getFlvContentEndpoint(): IEndpointInternal {
        if (flvContentEndpoint == null) {
            flvContentEndpoint = CompositeEndpoint(
                FlvMuxer(
                    isForFile = true
                ), ContentSink(context)
            )
        }
        return flvContentEndpoint!!
    }

    private fun getTsFileEndpoint(): IEndpointInternal {
        if (tsFileEndpoint == null) {
            tsFileEndpoint = CompositeEndpoint(
                TsMuxer(),
                FileSink(ioDispatcher)
            )
        }
        return tsFileEndpoint!!
    }

    private fun getTsContentEndpoint(): IEndpointInternal {
        if (tsContentEndpoint == null) {
            tsContentEndpoint = CompositeEndpoint(
                TsMuxer(), ContentSink(context)
            )
        }
        return tsContentEndpoint!!
    }

    private fun getSrtEndpoint(): IEndpointInternal {
        if (srtEndpoint == null) {
            srtEndpoint = CompositeEndpoints.createSrtEndpoint(null, ioDispatcher)
        }
        return srtEndpoint!!
    }

    private fun getRtmpEndpoint(): IEndpointInternal {
        if (rtmpEndpoint == null) {
            rtmpEndpoint = CompositeEndpoints.createRtmpEndpoint(ioDispatcher)
        }
        return rtmpEndpoint!!
    }

    companion object {
        private const val TAG = "DynamicEndpoint"
    }
}

/**
 * A factory to build a [DynamicEndpoint].
 */
class DynamicEndpointFactory : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: EncodingPipelineOutputDispatcherProvider
    ): IEndpointInternal = DynamicEndpoint(
        context,
        dispatcherProvider.defaultDispatcher,
        dispatcherProvider.ioDispatcher
    )
}
