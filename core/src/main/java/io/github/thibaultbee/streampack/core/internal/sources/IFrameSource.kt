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
package io.github.thibaultbee.streampack.core.internal.sources

import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.interfaces.Configurable
import io.github.thibaultbee.streampack.core.internal.interfaces.Releaseable
import io.github.thibaultbee.streampack.core.internal.interfaces.Streamable
import java.nio.ByteBuffer

interface IFrameSource<T> : Streamable, Configurable<T>, Releaseable {

    /**
     * Generate a frame from capture device
     * @param buffer buffer where to write data. Must be set as buffer of returned Frame
     * @return frame with correct infos (at least buffer, mime type and pts)
     */
    fun getFrame(buffer: ByteBuffer): io.github.thibaultbee.streampack.core.internal.data.Frame
}