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
package io.github.thibaultbee.streampack.core.elements.endpoints

import android.content.Context
import io.github.thibaultbee.streampack.core.configuration.mediadescriptor.MediaDescriptor
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.pipelines.IDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A dummy endpoint for testing.
 */
class DummyEndpoint : IEndpointInternal {
    private val _isOpenFlow = MutableStateFlow(false)
    override val isOpenFlow = _isOpenFlow.asStateFlow()

    private val _isStreamingFlow = MutableStateFlow(false)
    val isStreamingFlow = _isStreamingFlow.asStateFlow()

    override val info: IEndpoint.IEndpointInfo
        get() = TODO("Not yet implemented")

    override fun getInfo(type: MediaDescriptor.Type): IEndpoint.IEndpointInfo {
        TODO("Not yet implemented")
    }

    override val metrics: Any
        get() = TODO("Not yet implemented")
    override val throwableFlow: StateFlow<Throwable?> = MutableStateFlow(null).asStateFlow()

    override suspend fun open(descriptor: MediaDescriptor) {
        _isOpenFlow.emit(true)
    }

    override suspend fun close() {
        _isOpenFlow.emit(false)
    }

    override suspend fun write(frame: Frame, streamPid: Int) {
        frame.close()
    }

    override suspend fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        return streamConfigs.associateWith { it.hashCode() }
    }

    override suspend fun addStream(streamConfig: CodecConfig): Int {
        return streamConfig.hashCode()
    }

    override suspend fun startStream() {
        _isStreamingFlow.emit(true)
    }

    override suspend fun stopStream() {
        _isStreamingFlow.emit(false)
    }
}

/**
 * The factory to create [DummyEndpoint].
 */
class DummyEndpointFactory : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal {
        return DummyEndpoint()
    }
}

/**
 * The factory to create [DummyEndpoint].
 */
class DummyEndpointDummyFactory(val dummyEndpoint: DummyEndpoint) : IEndpointInternal.Factory {
    override fun create(
        context: Context,
        dispatcherProvider: IDispatcherProvider
    ): IEndpointInternal {
        return dummyEndpoint
    }
}