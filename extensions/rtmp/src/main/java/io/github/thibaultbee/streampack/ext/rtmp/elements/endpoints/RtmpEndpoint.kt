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
import io.github.komedia.komuxer.flv.tags.FLVTag
import io.github.komedia.komuxer.flv.tags.audio.AudioData
import io.github.komedia.komuxer.flv.tags.video.VideoData
import io.github.komedia.komuxer.rtmp.RtmpConnectionBuilder
import io.github.komedia.komuxer.rtmp.client.RtmpClient
import io.github.komedia.komuxer.rtmp.connect
import io.github.komedia.komuxer.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpoint
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint.EndpointInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ClosedException
import io.github.thibaultbee.streampack.core.elements.utils.ChannelWithCloseableData
import io.github.thibaultbee.streampack.core.elements.utils.useConsumeEach
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvTagBuilder
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An endpoint that send frame to an RTMP server.
 */
class RtmpEndpoint internal constructor(
    defaultDispatcher: CoroutineDispatcher,
    ioDispatcher: CoroutineDispatcher
) :
    IEndpointInternal {
    private val coroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val mutex = Mutex()

    private val flvTagChannel = ChannelWithCloseableData<FLVTag>(
        10 /* Arbitrary buffer size. TODO: add a parameter to set it */,
        BufferOverflow.DROP_OLDEST
    )
    private val flvTagBuilder = FlvTagBuilder(flvTagChannel)

    private val selectorManager = SelectorManager(ioDispatcher)
    private val connectionBuilder = RtmpConnectionBuilder(selectorManager)
    private var rtmpClient: RtmpClient? = null


    private var startUpTimestamp = INVALID_TIMESTAMP
    private val timestampMutex = Mutex()

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    override val info: IEndpoint.IEndpointInfo = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = EndpointInfo(FlvMuxerInfo)

    private val _throwableFlow = MutableStateFlow<Throwable?>(null)
    override val throwableFlow: StateFlow<Throwable?> = _throwableFlow.asStateFlow()

    init {
        coroutineScope.launch {
            flvTagChannel.useConsumeEach { flvTag ->
                write(flvTag)
            }
        }
    }

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

    private suspend fun write(flvTag: FLVTag) {
        try {
            safeClient { rtmpClient ->
                rtmpClient.write(flvTag)
            }
        } catch (_: TimeoutCancellationException) {
            Logger.w(TAG, "Frame dropped due to timeout")
        } catch (t: Throwable) {
            Logger.e(TAG, "Error while writing RTMP data: $t")
            if (isOpenFlow.value) {
                _throwableFlow.emit(ClosedException(t))
                close()
            }
        }
        try {
            if (flvTag.data is AudioData) {
                (flvTag.data as AudioData).body.close()
            } else if (flvTag.data is VideoData) {
                (flvTag.data as VideoData).body.close()
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Error while closing FLVTag data: $t")
        }
    }

    override suspend fun write(
        closeableFrame: FrameWithCloseable, streamPid: Int
    ) {
        val frame = closeableFrame.frame
        val startUpTimestamp = getStartUpTimestamp(frame.ptsInUs)
        val ts = (frame.ptsInUs - startUpTimestamp) / 1000
        if (ts < 0) {
            Logger.w(
                TAG,
                "Negative timestamp $ts for frame $frame. Frame will be dropped."
            )
            closeableFrame.close()
            return
        }
        flvTagBuilder.write(closeableFrame, ts.toInt(), streamPid)
    }

    override suspend fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        require(streamConfigs.isNotEmpty()) { "At least one stream must be provided" }
        return mutex.withLock {
            flvTagBuilder.addStreams(streamConfigs)
        }
    }

    override suspend fun addStream(streamConfig: CodecConfig) = mutex.withLock {
        flvTagBuilder.addStream(streamConfig)
    }

    override suspend fun startStream() {
        safeClient { rtmpClient ->
            rtmpClient.createStream()
            rtmpClient.publish(StreamPublishType.LIVE)

            rtmpClient.writeSetDataFrame(
                flvTagBuilder.metadata
            )
        }
    }

    override suspend fun stopStream() {
        mutex.withLock {
            try {
                if (rtmpClient?.isClosed == false) {
                    rtmpClient?.deleteStream()
                }
            } catch (t: Throwable) {
                Logger.w(TAG, "Error while stopping stream: $t")
            } finally {
                flvTagBuilder.clearStreams()
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
        coroutineScope.cancel()
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
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal = RtmpEndpoint(dispatcherProvider.default, dispatcherProvider.io)
}
