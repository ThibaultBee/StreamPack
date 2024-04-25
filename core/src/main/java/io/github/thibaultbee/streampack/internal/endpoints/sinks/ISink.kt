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
package io.github.thibaultbee.streampack.internal.endpoints.sinks

import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.interfaces.Configurable
import io.github.thibaultbee.streampack.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.internal.interfaces.SuspendStreamable

interface ISink : IPublicSink, SuspendStreamable, Configurable<Int>, Releaseable {

    /**
     * Writes a buffer to the [ISink].
     * @param packet buffer to write
     */
    fun write(packet: Packet)
}

interface IPublicSink