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
import io.github.thibaultbee.streampack.core.elements.data.deepCopy
import io.github.thibaultbee.streampack.core.elements.processing.IProcessor
import io.github.thibaultbee.streampack.core.elements.utils.pool.IBufferPool
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.IntFunction
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Audio frame processor.
 *
 * Supports mute effect and audio level monitoring.
 */
class AudioFrameProcessor(
    private val bufferPool: IBufferPool<ByteBuffer>,
    dispatcher: CoroutineDispatcher,
    private val effects: CopyOnWriteArrayList<IAudioEffect> = CopyOnWriteArrayList()
) : IProcessor<RawFrame>, IAudioFrameProcessor, Closeable, MutableList<IAudioEffect> by effects {
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Whether the audio is muted.
     *
     * When the audio is muted, the audio effect are not processed. Only consumer effects are processed.
     */
    override var isMuted = false
    override var channelCount = 1
    private val muteEffect = MuteEffect()
    
    override var audioLevelCallback: AudioLevelCallback? = null

    private fun launchConsumerEffect(
        effect: IConsumerAudioEffect,
        isMuted: Boolean,
        data: RawFrame
    ) {
        val consumeFrame = data.deepCopy(bufferPool)
        coroutineScope.launch {
            effect.consume(isMuted, consumeFrame)
        }
    }

    override fun process(data: RawFrame): RawFrame {
        val isMuted = isMuted

        audioLevelCallback?.let { callback ->
            val levels = calculateAudioLevels(data.rawBuffer, channelCount)
            callback(levels)
        }

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
    override fun <T> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?> {
        return super<IAudioFrameProcessor>.toArray(generator)
    }

    private fun calculateAudioLevels(buffer: ByteBuffer, channels: Int): AudioLevelData {
        val position = buffer.position()
        val limit = buffer.limit()
        val remaining = limit - position

        if (remaining < 2) {
            return AudioLevelData(channels, 0f, 0f, 0f, 0f)
        }

        val readBuffer = buffer.duplicate()
        readBuffer.position(position)
        readBuffer.order(ByteOrder.LITTLE_ENDIAN)

        var maxSampleLeft = 0
        var maxSampleRight = 0
        var sumSquaresLeft = 0.0
        var sumSquaresRight = 0.0
        var sampleCountLeft = 0
        var sampleCountRight = 0

        val isStereo = channels >= 2
        var isLeftChannel = true

        while (readBuffer.remaining() >= 2) {
            val sample = readBuffer.short.toInt()
            val absSample = abs(sample)
            val sampleSquared = (sample.toLong() * sample.toLong()).toDouble()

            if (!isStereo || isLeftChannel) {
                if (absSample > maxSampleLeft) maxSampleLeft = absSample
                sumSquaresLeft += sampleSquared
                sampleCountLeft++
            } else {
                if (absSample > maxSampleRight) maxSampleRight = absSample
                sumSquaresRight += sampleSquared
                sampleCountRight++
            }

            if (isStereo) isLeftChannel = !isLeftChannel
        }

        val peakLeft = if (sampleCountLeft > 0) (maxSampleLeft / 32767f).coerceIn(0f, 1f) else 0f
        val rmsLeft = if (sampleCountLeft > 0) (sqrt(sumSquaresLeft / sampleCountLeft) / 32767.0).toFloat().coerceIn(0f, 1f) else 0f

        val peakRight = if (sampleCountRight > 0) (maxSampleRight / 32767f).coerceIn(0f, 1f) else 0f
        val rmsRight = if (sampleCountRight > 0) (sqrt(sumSquaresRight / sampleCountRight) / 32767.0).toFloat().coerceIn(0f, 1f) else 0f

        return AudioLevelData(channels, rmsLeft, peakLeft, rmsRight, peakRight)
    }
}
