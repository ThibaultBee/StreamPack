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
package com.github.thibaultbee.streampack.internal.endpoints

import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.interfaces.Streamable

interface IEndpoint : Streamable<Int> {

    /**
     * Configure endpoint bitrate, mainly for network endpoint.
     * @param startBitrate bitrate at the beginning of the communication
     */
    override fun configure(startBitrate: Int)

    /**
     * Writes a buffer to endpoint.
     * @param packet buffer to write
     */
    fun write(packet: Packet)
}