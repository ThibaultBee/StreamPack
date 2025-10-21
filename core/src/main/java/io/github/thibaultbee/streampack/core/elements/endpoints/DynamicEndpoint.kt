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
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoints
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TsMuxer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val coroutineScope = CoroutineScope(defaultDispatcher)
    private val mutex = Mutex()

    // Current endpoint
    private val endpointFlow = MutableStateFlow<IEndpointInternal?>(null)
    private val endpoint: IEndpointInternal?
        get() = endpointFlow.value

    // Endpoints
    private var mediaMuxerEndpoint: IEndpointInternal? = null
    private var flvFileEndpoint: IEndpointInternal? = null
    private var flvContentEndpoint: IEndpointInternal? = null
    private var tsFileEndpoint: IEndpointInternal? = null
    private var tsContentEndpoint: IEndpointInternal? = null
    private var srtEndpoint: IEndpointInternal? = null
    private var rtmpEndpoint: IEndpointInternal? = null

    private val isOpenFlows = endpointFlow.map { it?.isOpenFlow }
    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow: StateFlow<Boolean> = _isOpenFlow.asStateFlow()

    /**
     * Only available when the endpoint is opened.
     */
    override val info: IEndpoint.IEndpointInfo
        get() = endpoint?.info ?: throw IllegalStateException("Endpoint is not opened")

    override fun getInfo(type: MediaDescriptor.Type) = getEndpoint(type).getInfo(type)

    override val metrics: Any
        get() = endpoint?.metrics ?: throw IllegalStateException("Endpoint is not opened")

    init {
        coroutineScope.launch {
            isOpenFlows.collect { isOpenFlow ->
                _isOpenFlow.emit(isOpenFlow?.value == true)
            }
        }
    }

    private suspend fun safeEndpoint(block: suspend (IEndpointInternal) -> Unit) {
        val endpoint = requireNotNull(endpoint) { "Not opened" }
        block(endpoint)
    }

    override suspend fun open(descriptor: MediaDescriptor) {
        mutex.withLock {
            if (isOpenFlow.value) {
                Logger.w(TAG, "DynamicEndpoint is already opened")
                return
            }

            val endpoint = prepareEndpoint(descriptor)
            endpoint.open(descriptor)
            _isOpenFlow.emit(true)
            endpointFlow.emit(endpoint)
        }
    }

    override fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        require(streamConfigs.isNotEmpty()) { "At least one stream config must be provided" }
        mutex.tryLock()
        return try {
            val endpoint = endpoint ?: throw IllegalStateException("Endpoint is not opened")
            endpoint.addStreams(streamConfigs)
        } finally {
            mutex.unlock()
        }
    }

    override fun addStream(streamConfig: CodecConfig): Int {
        mutex.tryLock()
        return try {
            val endpoint = endpoint ?: throw IllegalStateException("Endpoint is not opened")
            endpoint.addStream(streamConfig)
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun write(closeableFrame: FrameWithCloseable, streamPid: Int) =
        safeEndpoint { endpoint -> endpoint.write(closeableFrame, streamPid) }

    override suspend fun startStream() = safeEndpoint { endpoint -> endpoint.startStream() }

    override suspend fun stopStream() {
        mutex.withLock {
            endpoint?.stopStream()
        }
    }

    override suspend fun close() {
        mutex.withLock {
            try {
                endpoint?.close()
            } finally {
                _isOpenFlow.emit(false)
                endpointFlow.emit(null)
            }
        }
    }

    override fun release() {
        runBlocking {
            close()
        }
        coroutineScope.coroutineContext.cancelChildren()
    }

    private fun prepareEndpoint(mediaDescriptor: MediaDescriptor): IEndpointInternal {
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
                ), FileSink(ioDispatcher)
            )
        }
        return flvFileEndpoint!!
    }

    private fun getFlvContentEndpoint(): IEndpointInternal {
        if (flvContentEndpoint == null) {
            flvContentEndpoint = CompositeEndpoint(
                FlvMuxer(
                    isForFile = true
                ), ContentSink(context, ioDispatcher)
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
                TsMuxer(), ContentSink(context, ioDispatcher)
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
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal = DynamicEndpoint(
        context,
        dispatcherProvider.default,
        dispatcherProvider.io
    )
}
