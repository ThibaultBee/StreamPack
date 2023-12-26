/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.ext.srt.streamers.interfaces

import io.github.thibaultbee.streampack.ext.srt.data.SrtConnectionDescriptor
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer

interface ISrtLiveStreamer : ILiveStreamer {
    /**
     * Get/set connection passphrase.
     */
    var passPhrase: String

    /**
     * Get/set stream id.
     */
    var streamId: String

    /**
     * Get/set bidirectional latency in milliseconds.
     */
    val latency: Int

    /**
     * Connect to a remote server.
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed
     */
    @Deprecated(
        "Use connect(SrtConnectionDescriptor) instead",
        replaceWith = ReplaceWith("connect(SrtConnectionDescriptor)")
    )
    suspend fun connect(ip: String, port: Int)

    /**
     * Connect to a remote server.
     *
     * @param connection the SRT connection
     * @throws Exception if connection has failed or configuration has failed
     */
    suspend fun connect(connection: SrtConnectionDescriptor)

    /**
     * Same as [connect] then [startStream].
     *
     * @param ip server ip
     * @param port server port
     * @throws Exception if connection has failed or configuration has failed or startStream failed.
     */
    @Deprecated(
        "Use startStream(SrtConnectionDescriptor) instead",
        replaceWith = ReplaceWith("startStream(SrtConnectionDescriptor)")
    )
    suspend fun startStream(ip: String, port: Int)

    /**
     * Same as [connect] then [startStream].
     *
     * @param connection the SRT connection
     * @throws Exception if connection has failed or configuration has failed or startStream failed.
     */
    suspend fun startStream(connection: SrtConnectionDescriptor)
}