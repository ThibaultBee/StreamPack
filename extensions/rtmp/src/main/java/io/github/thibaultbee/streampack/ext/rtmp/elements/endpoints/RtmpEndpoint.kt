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
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ClosedException
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvDataBuilder
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An endpoint that send frame to an RTMP server.
 */
class RtmpEndpoint internal constructor() : IEndpointInternal {
    private val flvDataBuilder = FlvDataBuilder()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val connectionBuilder = RtmpConnectionBuilder(selectorManager)
    private var rtmpClient: RtmpClient? = null
    private val mutex = Mutex()

    private var startUpTimestamp = INVALID_TIMESTAMP
    private val timestampMutex = Mutex()

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    override val info: IEndpoint.IEndpointInfo = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = EndpointInfo(FlvMuxerInfo)

    private suspend fun <T> safeClient(block: suspend (RtmpClient) -> T): T {
        val rtmpClient = requireNotNull(rtmpClient) { "Not opened" }
        require(!rtmpClient.isClosed) { "Connection closed" }
        return mutex.withLock { block(rtmpClient) }
    }

    override suspend fun open(descriptor: MediaDescriptor) {
        mutex.withLock {
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
    }

    private suspend fun getStartUpTimestamp(ptsInUs: Long): Long = timestampMutex.withLock {
        // FLV timestamps start at 0
        if (startUpTimestamp == INVALID_TIMESTAMP) {
            startUpTimestamp = ptsInUs
        }
        startUpTimestamp
    }

    override suspend fun write(frame: Frame, streamPid: Int) {
        val startUpTimestamp = getStartUpTimestamp(frame.ptsInUs)
        val ts = (frame.ptsInUs - startUpTimestamp) / 1000
        if (ts < 0) {
            Logger.w(TAG, "Negative timestamp, dropping frame")
            return
        }
        try {
            flvDataBuilder.write(frame, streamPid).forEach { flvData ->
                safeClient { rtmpClient ->
                    rtmpClient.write(
                        flvData,
                        ts.toInt(),
                    ).await()
                }
                if (flvData is AutoCloseable) {
                    flvData.close()
                }
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Error while writing packet to socket", t)
            if (isOpenFlow.value) {
                close()
                throw ClosedException(t)
            }
        }
    }

    override fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        require(streamConfigs.isNotEmpty()) { "At least one stream must be provided" }
        mutex.tryLock()
        return try {
            flvDataBuilder.addStreams(streamConfigs)
        } finally {
            mutex.unlock()
        }
    }

    override fun addStream(streamConfig: CodecConfig): Int {
        mutex.tryLock()
        return try {
            flvDataBuilder.addStream(streamConfig)
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun startStream() {
        safeClient { rtmpClient ->
            rtmpClient.createStream()
            rtmpClient.publish(StreamPublishType.LIVE)

            rtmpClient.writeSetDataFrame(
                flvDataBuilder.metadata
            )
        }.await()
    }

    override suspend fun stopStream() {
        mutex.withLock {
            try {
                rtmpClient?.deleteStream()
            } catch (t: Throwable) {
                Logger.w(TAG, "Error while stopping stream: $t")
            } finally {
                flvDataBuilder.clearStreams()
            }
        }
        timestampMutex.withLock {
            startUpTimestamp = INVALID_TIMESTAMP
        }
    }

    override suspend fun close() {
        mutex.withLock {
            try {
                rtmpClient?.close()
            } finally {
                rtmpClient = null
            }
        }
    }

    override fun release() {
        selectorManager.close()
    }

    companion object {
        private const val TAG = "RtmpEndpoint"

        private const val INVALID_TIMESTAMP = -1L
    }
}

/**
 * A factory to build a [RtmpEndpoint].
 */
class RtmpEndpointFactory : IEndpointInternal.Factory {
    override fun create(context: Context): IEndpointInternal = RtmpEndpoint()
}
