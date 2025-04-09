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

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.utils.pool.IRawFrameFactory
import io.github.thibaultbee.streampack.core.elements.utils.pool.IReadOnlyRawFrameFactory

interface IAudioFrameSourceInternal {
    /**
     * Gets an audio frame from the source.
     *
     * @param frame the [RawFrame] to fill with audio data.
     * @return a [RawFrame] containing audio data.
     */
    fun fillAudioFrame(frame: RawFrame): RawFrame

    /**
     * Gets an audio frame from the source.
     *
     * The [RawFrame] to fill with audio data is created by the [frameFactory].
     *
     * @param frameFactory a [IRawFrameFactory] to create [RawFrame].
     * @return a [RawFrame] containing audio data.
     */
    fun getAudioFrame(frameFactory: IReadOnlyRawFrameFactory): RawFrame
}