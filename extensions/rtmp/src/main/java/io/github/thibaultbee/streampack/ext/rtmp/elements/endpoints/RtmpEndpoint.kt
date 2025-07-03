/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.ext.rtmp.elements.endpoints

import android.content.Context
import io.github.thibaultbee.krtmp.rtmp.RtmpConnectionBuilder
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClient
import io.github.thibaultbee.krtmp.rtmp.connect
import io.github.thibaultbee.krtmp.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint.EndpointInfo
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvFilter
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RtmpEndpoint : IEndpointInternal {
    private val flvFilter = FlvFilter()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val connectionBuilder = RtmpConnectionBuilder(selectorManager)
    private var rtmpClient: RtmpClient? = null

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    override val info: IEndpoint.IEndpointInfo = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = EndpointInfo(FlvMuxerInfo)

    override suspend fun open(descriptor: MediaDescriptor) {
        if (rtmpClient?.isClosed == false) {
            Logger.w(TAG, "Already opened")
            return
        }

        rtmpClient = connectionBuilder.connect(descriptor.uri.toString()).apply {
            _isOpenFlow.emit(true)

            socketContext.invokeOnCompletion {
                _isOpenFlow.tryEmit(false)
            }
        }
    }

    override suspend fun write(frame: Frame, streamPid: Int) {
        val rtmpClient = requireNotNull(rtmpClient) { "Not opened" }
        require(!rtmpClient.isClosed) { "Connection closed" }

        flvFilter.write(frame, streamPid).forEach { flvData ->
            rtmpClient.write((frame.ptsInUs / 1000).toInt(), flvData)
            if (flvData is AutoCloseable) {
                flvData.close()
            }
        }
    }

    override fun addStreams(streamConfigs: List<CodecConfig>) = flvFilter.addStreams(streamConfigs)

    override fun addStream(streamConfig: CodecConfig) = flvFilter.addStream(streamConfig)

    override suspend fun startStream() {
        val rtmpClient = requireNotNull(rtmpClient) { "Not opened" }
        rtmpClient.createStream()
        rtmpClient.publish(StreamPublishType.LIVE)

        rtmpClient.writeSetDataFrame(
            flvFilter.metadata
        )
    }

    override suspend fun stopStream() {
        try {
            rtmpClient?.deleteStream()
        } catch (t: Throwable) {
            Logger.w(TAG, "Error while stopping stream: $t")
        }
        flvFilter.clearStreams()
    }

    override suspend fun close() {
        rtmpClient?.close()
        rtmpClient = null
    }

    override fun release() {
        selectorManager.close()
    }

    companion object {
        private const val TAG = "RtmpEndpoint"
    }
}

/**
 * A factory to build a [RtmpEndpoint].
 */
class RtmpEndpointFactory : IEndpointInternal.Factory {
    override fun create(context: Context): IEndpointInternal = RtmpEndpoint()
}
