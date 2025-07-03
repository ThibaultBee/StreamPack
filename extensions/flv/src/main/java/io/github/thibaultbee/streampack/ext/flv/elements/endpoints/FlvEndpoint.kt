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
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.IEndpointInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.MediaSinkType
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.CompositeEndpoint.EndpointInfo
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.sinks.ContentSink
import io.github.thibaultbee.streampack.core.logger.Logger
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.FlvMuxerInfo
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvFilter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Writes FLV Data to a file or content.
 */
sealed class FlvEndpoint : IEndpointInternal {
    private val flvFilter = FlvFilter()
    private var flvMuxer: FLVMuxer? = null
    private val mutex = Mutex()

    override val metrics: Any
        get() = TODO("Not yet implemented")

    override val isOpenFlow: StateFlow<Boolean>
        get() = TODO("Not yet implemented")
    override val info = EndpointInfo(FlvMuxerInfo)

    override fun getInfo(type: MediaDescriptor.Type) = info

    abstract suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer
    override suspend fun open(descriptor: MediaDescriptor) {
        mutex.withLock {
            if (flvMuxer != null) {
                Logger.w(TAG, "Already opened")
                return
            }
            flvMuxer = openImpl(descriptor)
        }
    }

    override suspend fun write(
        frame: Frame, streamPid: Int
    ) {
        mutex.withLock {
            flvFilter.write(frame, streamPid).forEach {
                flvMuxer!!.encode(FLVTag((frame.ptsInUs / 1000).toInt(), it))
            }
        }
    }

    override fun addStreams(streamConfigs: List<CodecConfig>) = flvFilter.addStreams(streamConfigs)

    override fun addStream(streamConfig: CodecConfig) = flvFilter.addStream(streamConfig)

    override suspend fun startStream() {
        mutex.withLock {
            flvMuxer!!.encodeFLVHeader(flvFilter.hasAudio, flvFilter.hasVideo)
        }
    }

    override suspend fun stopStream() {
        mutex.withLock {
            flvMuxer?.flush()
        }
    }

    override suspend fun close() {
        mutex.withLock {
            flvMuxer?.close()
            flvMuxer = null
        }
    }

    companion object {
        private const val TAG = "FlvEndpoint"
    }
}


/**
 * Writes FLV Data to a content.
 */
class FlvContentEndpoint(private val context: Context) : FlvEndpoint() {
    override suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer {
        require(descriptor.type.sinkType == MediaSinkType.CONTENT) { "Descriptor type must be ${MediaSinkType.CONTENT}" }
        return FLVMuxer(ContentSink.openContent(context, descriptor.uri), AmfVersion.AMF0)
    }
}


/**
 * Writes FLV Data to a file.
 */
class FlvFileEndpoint : FlvEndpoint() {
    override suspend fun openImpl(descriptor: MediaDescriptor): FLVMuxer {
        require(descriptor.type.sinkType == MediaSinkType.FILE) { "Descriptor type must be ${MediaSinkType.FILE}" }
        return FLVMuxer(descriptor.uri.path!!, AmfVersion.AMF0)
    }
}



