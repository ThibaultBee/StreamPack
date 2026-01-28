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
package io.github.thibaultbee.streampack.core.elements.processing.audio

import io.github.thibaultbee.streampack.core.elements.data.RawFrame
import io.github.thibaultbee.streampack.core.elements.processing.IEffectConsumer
import io.github.thibaultbee.streampack.core.elements.processing.IEffectProcessor
import java.io.Closeable

/**
 * The base audio effect.
 */
sealed interface IAudioEffect : Closeable

/**
 * An audio effect that can be dispatched to another thread. The result is not use by the audio pipeline.
 * Example: a VU meter.
 */
interface IConsumerAudioEffect : IAudioEffect, IEffectConsumer<RawFrame>

/**
 * An audio effect that can't be dispatched to another thread. The result is used by the audio pipeline.
 *
 * The [RawFrame.rawBuffer] can't be modified.
 */
interface IProcessorAudioEffect : IAudioEffect, IEffectProcessor<RawFrame>

/**
 * An audio effect that mute the audio.
 */
class MuteEffect : IProcessorAudioEffect {
    private var mutedByteArray: ByteArray? = null

    override fun process(isMuted: Boolean, data: RawFrame): RawFrame {
        if (!isMuted) {
            return data
        }
        val remaining = data.rawBuffer.remaining()
        val position = data.rawBuffer.position()
        if (remaining != mutedByteArray?.size) {
            mutedByteArray = ByteArray(remaining)
        }
        data.rawBuffer.put(mutedByteArray!!)
        data.rawBuffer.position(position)

        return data
    }

    override fun close() {
        mutedByteArray = null
    }
}