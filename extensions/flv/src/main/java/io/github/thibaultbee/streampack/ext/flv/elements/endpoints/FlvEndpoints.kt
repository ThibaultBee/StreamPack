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
import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.FLVMuxer
import io.github.thibaultbee.krtmp.flv.encode
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.data.useAndUnwrap
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint.EndpointInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvDataBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Writes FLV Data to a file or content.
 */
sealed class FlvEndpoint(private val coroutineDispatcher: CoroutineDispatcher) : IEndpointInternal {
    private val flvDataBuilder = FlvDataBuilder()
    private var flvMuxer: FLVMuxer? = null
    private val mutex = Mutex()

    private var startUpTimestamp = INVALID_TIMESTAMP
    private val timestampMutex = Mutex()

    override val metrics: Any
        get() = TODO("Not yet implemented")

    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow: StateFlow<Boolean> = _isOpenFlow.asStateFlow()

    override val throwableFlow: StateFlow<Throwable?> = MutableStateFlow(null).asStateFlow()

    override val info = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = info

    private suspend fun safeMuxer(block: suspend (FLVMuxer) -> Unit) {
        val flvMuxer = requireNotNull(flvMuxer) { "Not opened" }
        mutex.withLock { block(flvMuxer) }
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

    override suspend fun write(
        closeableFrame: FrameWithCloseable, streamPid: Int
    ) {
        closeableFrame.useAndUnwrap { frame ->
            val startUpTimestamp = getStartUpTimestamp(frame.ptsInUs)
            val ts = (frame.ptsInUs - startUpTimestamp) / 1000
            flvDataBuilder.write(frame, streamPid).forEach {
                safeMuxer { flvMuxer ->
                    flvMuxer.encode(FLVTag(ts.toInt(), it))
                }
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
        safeMuxer { flvMuxer ->
            flvMuxer.encodeFLVHeader(flvDataBuilder.hasAudio, flvDataBuilder.hasVideo)
            flvMuxer.encode(0, OnMetadata(flvDataBuilder.metadata))
        }
    }

    override suspend fun stopStream() {
        mutex.withLock {
            try {
                flvMuxer?.flush()
            } catch (t: Throwable) {
                Logger.w(TAG, "Error while flushing FLV muxer: $t")
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
                flvMuxer?.close()
            } finally {
                _isOpenFlow.emit(false)
                flvMuxer = null
            }
        }
    }

    companion object {
        private const val TAG = "FlvEndpoint"

        private const val INVALID_TIMESTAMP = -1L
    }
}


/**
 * Writes FLV Data to a content.
 */
class FlvContentEndpoint(private val context: Context, coroutineDispatcher: CoroutineDispatcher) :
    FlvEndpoint(coroutineDispatcher) {
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
    ): IEndpointInternal = FlvContentEndpoint(context, dispatcherProvider.io)
}


/**
 * Writes FLV Data to a file.
 */
class FlvFileEndpoint(coroutineDispatcher: CoroutineDispatcher) : FlvEndpoint(coroutineDispatcher) {
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
    ): IEndpointInternal = FlvFileEndpoint(dispatcherProvider.io)
}




