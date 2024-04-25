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
package io.github.thibaultbee.streampack.internal.endpoints.composites

import io.github.thibaultbee.streampack.internal.endpoints.IConnectableEndpoint
import io.github.thibaultbee.streampack.internal.endpoints.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.endpoints.sinks.IConnectable
import io.github.thibaultbee.streampack.internal.endpoints.sinks.ILiveSink
import io.github.thibaultbee.streampack.listeners.OnConnectionListener

/**
 * A [CompositeEndpoint] with [IConnectable] capabilities.
 */
class ConnectableCompositeEndpoint(muxer: IMuxer, override val sink: ILiveSink) :
    CompositeEndpoint(muxer, sink), IConnectableEndpoint {

    override var onConnectionListener: OnConnectionListener?
        get() = sink.onConnectionListener
        set(value) {
            sink.onConnectionListener = value
        }

    override val isConnected: Boolean
        get() = sink.isConnected

    override suspend fun connect(url: String) = sink.connect(url)

    override fun disconnect() = sink.disconnect()
}