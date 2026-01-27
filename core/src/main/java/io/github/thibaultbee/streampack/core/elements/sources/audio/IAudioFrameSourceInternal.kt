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
package io.github.thibaultbee.streampack.core.elements.sources.audio

import java.nio.ByteBuffer

interface IAudioFrameSourceInternal {
    /**
     * Gets the size of the buffer to allocate.
     * When using encoder is callback mode, it's unused.
     */
    val minBufferSize: Int

    /**
     * Gets an audio frame from the source.
     *
     * @param buffer the [ByteBuffer] to fill with audio data.
     * @return the timestamp in microseconds.
     */
    fun fillAudioFrame(buffer: ByteBuffer): Long
}