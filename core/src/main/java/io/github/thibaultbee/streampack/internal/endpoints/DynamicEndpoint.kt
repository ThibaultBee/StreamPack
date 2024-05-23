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

import android.content.Context
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.RtmpMediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.SrtMediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.UriMediaDescriptor
import io.github.thibaultbee.streampack.data.mediadescriptor.createDefaultTsServiceInfo
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.endpoints.composites.CompositeEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.composites.CompositeEndpoints
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.flv.FlvMuxer
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.TSMuxer
import io.github.thibaultbee.streampack.internal.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.FileSink
import io.github.thibaultbee.streampack.internal.endpoints.composites.sinks.ISink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Default implementation of [IEndpoint].
 *
 * It creates an [IEndpoint] based on the [MediaDescriptor].
 */
class DynamicEndpoint(
    private val context: Context,
    private val factory: Factory = DefaultFactory(context)
) : IEndpoint {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var _endpoint: IEndpoint? = null

    private val endpoint: IEndpoint
        get() {
            val endpoint = _endpoint
            require(endpoint != null) { "Set an endpoint before trying to write to it" }
            return endpoint
        }


    private var _isOpened = MutableStateFlow(false)

    override val isOpened: StateFlow<Boolean> = _isOpened

    /**
     * Only available when the endpoint is opened.
     */
    override val info: IPublicEndpoint.IEndpointInfo
        get() = _endpoint?.info ?: throw IllegalStateException("Endpoint is not opened")

    fun getInfo(descriptor: MediaDescriptor): IPublicEndpoint.IEndpointInfo {
        return factory.create(descriptor).info // TODO: to optimize
    }

    override suspend fun open(descriptor: MediaDescriptor) {
        require(!_isOpened.value) { "Endpoint is already opened" }

        val endpoint = factory.create(descriptor)
        coroutineScope.launch { endpoint.isOpened.collect { _isOpened.emit(it) } }//TODO: fix this
        endpoint.open(descriptor)
        _endpoint = endpoint
        _isOpened.emit(true) // TODO: fix this
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
            _endpoint = null
        }
    }

    override fun release() {
        _endpoint?.release()
    }

    /**
     * Factory to create an [IEndpoint] based on a [MediaDescriptor].
     */
    interface Factory {

        /**
         * Create an [IEndpoint] based on a [MediaDescriptor].
         */
        fun create(descriptor: MediaDescriptor): IEndpoint
    }

    /**
     * Default implementation of [Factory].
     */
    class DefaultFactory(private val context: Context) : Factory {
        override fun create(descriptor: MediaDescriptor): IEndpoint {
            return when (descriptor) {
                is UriMediaDescriptor -> createUriEndpoint(descriptor)
                is RtmpMediaDescriptor -> CompositeEndpoints.createRtmpEndpoint()
                is SrtMediaDescriptor -> {
                    val serviceInfo = descriptor.getCustomData(TSServiceInfo::class.java)!!
                    CompositeEndpoints.createSrtEndpoint(serviceInfo)
                }
            }
        }

        private fun createUriEndpoint(
            descriptor: UriMediaDescriptor
        ): IEndpoint {
            return when (descriptor.sinkType) {
                MediaSinkType.FILE -> createUriFileEndpoint(descriptor, FileSink())
                MediaSinkType.CONTENT -> createUriFileEndpoint(descriptor, ContentSink(context))
                MediaSinkType.SRT -> {
                    val serviceInfo = descriptor.getCustomData(TSServiceInfo::class.java)!!
                    CompositeEndpoints.createSrtEndpoint(serviceInfo)
                }

                MediaSinkType.RTMP -> CompositeEndpoints.createRtmpEndpoint()
            }
        }

        private fun createUriFileEndpoint(descriptor: UriMediaDescriptor, sink: ISink): IEndpoint {
            return when (descriptor.getContainerType(context)) {
                MediaContainerType.MP4 -> MediaMuxerEndpoint(context)
                MediaContainerType.TS -> {
                    val serviceInfo = descriptor.getCustomData(TSServiceInfo::class.java)
                        ?: createDefaultTsServiceInfo()
                    CompositeEndpoint(
                        TSMuxer().apply { addService(serviceInfo) },
                        sink
                    )
                }

                MediaContainerType.FLV -> CompositeEndpoint(
                    FlvMuxer(
                        isForFile = true
                    ), sink
                )
            }
        }
    }
}
