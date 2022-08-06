/*
 * Copyright (C) 2022 Thibault B.
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

import io.github.thibaultbee.streampack.listeners.OnConnectionListener

interface ILiveEndpoint : IEndpoint {
    /**
     * Listener to manage connection.
     */
    var onConnectionListener: OnConnectionListener?

    /**
     * Check if the endpoint is connected to the server.
     */
    val isConnected: Boolean

    /**
     * Connect to a server.
     * To avoid creating an unresponsive UI, do not call on main thread.
     *
     * @param url server url
     * @throws Exception if connection has failed or configuration has failed
     */
    suspend fun connect(url: String)

    /**
     * Disconnect from the remote server.
     *
     * @throws Exception is not connected
     */
    fun disconnect()
}