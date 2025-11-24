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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints

import android.content.Context
import io.github.komedia.komuxer.amf.AmfVersion
import io.github.komedia.komuxer.flv.FLVMuxer
import io.github.komedia.komuxer.flv.encode
import io.github.komedia.komuxer.flv.tags.FLVTag
import io.github.komedia.komuxer.flv.tags.script.OnMetadata
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint.EndpointInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.elements.utils.ChannelWithCloseableData
import io.github.thibaultbee.streampack.core.elements.utils.useConsumeEach
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvTagBuilder
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.close
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes FLV Data to a file or content.
 */
sealed class FlvEndpoint(
    defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher
) : IEndpointInternal {
    private val coroutineScope = CoroutineScope(defaultDispatcher)
    private val mutex = Mutex()

    private val flvTagChannel = ChannelWithCloseableData<FLVTag>(20, BufferOverflow.DROP_OLDEST)
    private val flvTagBuilder = FlvTagBuilder(flvTagChannel)
    private var flvMuxer: FLVMuxer? = null

    private var startUpTimestamp = INVALID_TIMESTAMP
    private val timestampMutex = Mutex()

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow: StateFlow<Boolean> = _isOpenFlow.asStateFlow()

    override val throwableFlow: StateFlow<Throwable?> = MutableStateFlow(null).asStateFlow()

    override val info = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = info

    init {
        coroutineScope.launch {
            flvTagChannel.useConsumeEach { flvTag ->
                try {
                    write(flvTag)
                } catch (t: Throwable) {
                    Logger.e(TAG, "Error while writing FLV data: $t")
                }
            }
        }
    }

    private suspend fun safeMuxer(block: suspend (FLVMuxer) -> Unit) {
        val flvMuxer = requireNotNull(flvMuxer) { "Not opened" }
        mutex.withLock { withContext(ioDispatcher) { block(flvMuxer) } }
    }

    abstract suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer

    override suspend fun open(descriptor: MediaDescriptor) {
        mutex.withLock {
            if (flvMuxer != null) {
                Logger.w(TAG, "Already opened")
                return
            }
            flvMuxer = openImpl(descriptor)
            _isOpenFlow.emit(true)
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
        safeMuxer { flvMuxer ->
            flvMuxer.encode(flvTag)
        }
        // Close FLVTag data if needed
        try {
            flvTag.data.close()
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
        flvTagBuilder.write(closeableFrame, ts.toInt(), streamPid)
    }

    override suspend fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        require(streamConfigs.isNotEmpty()) { "At least one stream must be provided" }
        return flvTagBuilder.addStreams(streamConfigs)
    }

    override suspend fun addStream(streamConfig: CodecConfig) =
        flvTagBuilder.addStream(streamConfig)

    override suspend fun startStream() {
        safeMuxer { flvMuxer ->
            flvMuxer.encodeFLVHeader(flvTagBuilder.hasAudio, flvTagBuilder.hasVideo)
            flvMuxer.encode(0, OnMetadata(flvTagBuilder.metadata))
        }
    }

    override suspend fun stopStream() {
        try {
            mutex.withLock {
                withContext(ioDispatcher) {
                    flvMuxer?.flush()
                }
            }
        } catch (t: Throwable) {
            Logger.w(TAG, "Error while flushing FLV muxer: $t")
        } finally {
            flvTagBuilder.clearStreams()
            timestampMutex.withLock {
                startUpTimestamp = INVALID_TIMESTAMP
            }
        }
    }

    override suspend fun close() {
        mutex.withLock {
            try {
                flvMuxer?.close()
            } finally {
                _isOpenFlow.emit(false)
                flvMuxer = null
            }
        }
    }

    override fun release() {
        flvTagChannel.cancel()
    }

    companion object {
        private const val TAG = "FlvEndpoint"

        private const val INVALID_TIMESTAMP = -1L
    }
}


/**
 * Writes FLV Data to a content.
 */
class FlvContentEndpoint(
    private val context: Context,
    defaultDispatcher: CoroutineDispatcher,
    ioDispatcher: CoroutineDispatcher
) :
    FlvEndpoint(defaultDispatcher, ioDispatcher) {
    override suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer {
        require(descriptor.type.sinkType == MediaSinkType.CONTENT) { "Descriptor type must be ${MediaSinkType.CONTENT}" }
        return FLVMuxer(ContentSink.openContent(context, descriptor.uri), AmfVersion.AMF0)
    }
}

/**
 * A factory to build a [FlvContentEndpoint].
 */
class FlvContentEndpointFactory : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal =
        FlvContentEndpoint(context, dispatcherProvider.default, dispatcherProvider.io)
}


/**
 * Writes FLV Data to a file.
 */
class FlvFileEndpoint(
    defaultDispatcher: CoroutineDispatcher,
    ioDispatcher: CoroutineDispatcher
) : FlvEndpoint(defaultDispatcher, ioDispatcher) {
    override suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer {
        require(descriptor.type.sinkType == MediaSinkType.FILE) { "Descriptor type must be ${MediaSinkType.FILE}" }
        return FLVMuxer(descriptor.uri.path!!, AmfVersion.AMF0)
    }
}

/**
 * A factory to build a [FlvFileEndpoint].
 */
class FlvFileEndpointFactory : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal = FlvFileEndpoint(dispatcherProvider.default, dispatcherProvider.io)
}




