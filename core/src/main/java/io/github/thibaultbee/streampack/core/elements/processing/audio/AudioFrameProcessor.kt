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
import io.github.thibaultbee.streampack.core.elements.data.copy
import io.github.thibaultbee.streampack.core.elements.processing.IProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.function.IntFunction

/**
 * Audio frame processor.
 *
 * It is not thread-safe.
 */
class AudioFrameProcessor(
    dispatcher: CoroutineDispatcher,
    private val effects: MutableList<IAudioEffect> = mutableListOf()
) : IProcessor<RawFrame>, IAudioFrameProcessor, Closeable, MutableList<IAudioEffect> by effects {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Whether the audio is muted.
     *
     * When the audio is muted, the audio effect are not processed. Only consumer effects are processed.
     */
    override var isMuted = false
    private val muteEffect = MuteEffect()

    private fun launchConsumerEffect(
        effect: IConsumerAudioEffect,
        isMuted: Boolean,
        data: RawFrame
    ) {
        val consumeFrame =
            data.copy(
                rawBuffer = data.rawBuffer.duplicate().asReadOnlyBuffer()
            )
        coroutineScope.launch {
            effect.consume(isMuted, consumeFrame)
        }
    }

    override fun process(data: RawFrame): RawFrame {
        val isMuted = isMuted

        var processedFrame = muteEffect.process(isMuted, data)

        effects.forEach {
            if (it is IProcessorAudioEffect) {
                processedFrame = it.process(isMuted, processedFrame)
            } else if (it is IConsumerAudioEffect) {
                launchConsumerEffect(it, isMuted, processedFrame)
            }
        }

        return processedFrame
    }

    override fun close() {
        effects.forEach { it.close() }
        effects.clear()

        muteEffect.close()

        coroutineScope.cancel()
    }

    @Deprecated("'fun <T : Any!> toArray(generator: IntFunction<Array<(out) T!>!>!): Array<(out) T!>!' is deprecated. This declaration is redundant in Kotlin and might be removed soon.")
    @Suppress("DEPRECATION")
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?> {
        return super.toArray(generator)
    }
}
