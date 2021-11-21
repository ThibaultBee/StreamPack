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
package io.github.thibaultbee.streampack.internal.utils

import android.media.AudioFormat
import com.github.thibaultbee.streampack.internal.utils.replaceAllBy
import io.github.thibaultbee.streampack.data.AudioConfig
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.sqrt

class Measurements {
    var audioConfig: AudioConfig? = null
        set(value) {
            _audio = value?.let {
                AudioMeasurements(
                    it.byteFormat,
                    AudioConfig.getNumberOfChannels(it.channelConfig)
                )
            }
            field = value
        }

    private var _audio: AudioMeasurements? = null
    val audio: AudioMeasurements?
        get() = _audio
}

class AudioMeasurements(private val byteFormat: Int, private val numOfChannel: Int) {
    private val fullScalePeakValue = when (byteFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> Byte.MAX_VALUE - Byte.MIN_VALUE
        AudioFormat.ENCODING_PCM_16BIT -> Short.MAX_VALUE - Short.MIN_VALUE
        AudioFormat.ENCODING_PCM_32BIT -> Int.MAX_VALUE - Int.MIN_VALUE.toLong()
        AudioFormat.ENCODING_PCM_FLOAT -> 1
        else -> throw IOException("Byte format $byteFormat is not supported")
    } as Number
    private val resetValue = when (byteFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> 0.toByte()
        AudioFormat.ENCODING_PCM_16BIT -> 0.toShort()
        AudioFormat.ENCODING_PCM_32BIT -> 0
        AudioFormat.ENCODING_PCM_FLOAT -> 0F
        else -> throw IOException("Byte format $byteFormat is not supported")
    } as Number
    private var enableRms: Boolean = false
    private var squareSum = MutableList(numOfChannel) { 0F }
    private var numOfSample: Int = 0
    private val rmsSync = Any()

    /**
     * Get the RMS dBFS
     *
     * First value is always 0. Then call this method periodically.
     *
     * @return the RMS dBFS. Can be -Infinity if not audio has been captured since last call.
     */
    val rms: List<Float>
        get() {
            enableRms = true
            return synchronized(rmsSync) {
                val result = squareSum.map {
                    20 * log10(sqrt(it / numOfSample) * sqrt(2F) / fullScalePeakValue.toFloat())
                }
                numOfSample = 0
                squareSum.replaceAllBy(0F)
                result
            }
        }

    private var enablePeak: Boolean = false
    private var maxPeakValue = MutableList<Number>(numOfChannel) { 0 }
    private val peakSync = Any()

    /**
     * Get the Peak dBFS
     *
     * First value is always 0. Then call this method periodically.
     *
     * @return the peak dBFS. Can be NaN if not audio has been captured since last call.
     */
    val peak: List<Float>
        get() {
            enablePeak = true
            return synchronized(peakSync) {
                val result = maxPeakValue.map {
                    20 * log10(it.toFloat() / fullScalePeakValue.toFloat())
                }
                maxPeakValue.replaceAllBy(resetValue)
                result
            }
        }

    internal fun onNewBuffer(buffer: ByteBuffer) {
        if (enablePeak && enableRms) {
            synchronized(peakSync) {
                when (byteFormat) {
                    AudioFormat.ENCODING_PCM_8BIT -> buffer.getMaxValueAndSquareSumPerChannel(
                        numOfChannel
                    ).apply {
                        maxPeakValue = first.mapIndexed { index, value ->
                            value.coerceAtLeast(maxPeakValue[index] as Byte)
                        }.toMutableList()
                        squareSum =
                            second.mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                        numOfSample += buffer.remaining() / numOfChannel
                    }
                    AudioFormat.ENCODING_PCM_16BIT -> buffer.asShortBuffer()
                        .getMaxValueAndSquareSumPerChannel(numOfChannel).apply {
                            maxPeakValue = first.mapIndexed { index, value ->
                                value.coerceAtLeast(maxPeakValue[index] as Short)
                            }.toMutableList()
                            squareSum = second.mapIndexed { index, fl -> fl + squareSum[index] }
                                .toMutableList()
                            numOfSample += buffer.remaining() / (numOfChannel * Short.SIZE_BYTES)
                        }
                    AudioFormat.ENCODING_PCM_32BIT -> buffer.asIntBuffer()
                        .getMaxValueAndSquareSumPerChannel(numOfChannel).apply {
                            maxPeakValue = first.mapIndexed { index, value ->
                                value.coerceAtLeast(maxPeakValue[index] as Int)
                            }.toMutableList()
                            squareSum = second.mapIndexed { index, fl -> fl + squareSum[index] }
                                .toMutableList()
                            numOfSample += buffer.remaining() / (numOfChannel * Int.SIZE_BYTES)
                        }

                    AudioFormat.ENCODING_PCM_FLOAT -> buffer.asFloatBuffer()
                        .getMaxValueAndSquareSumPerChannel(numOfChannel).apply {
                            maxPeakValue = first.mapIndexed { index, value ->
                                value.coerceAtLeast(maxPeakValue[index] as Float)
                            }.toMutableList()
                            squareSum = buffer.asFloatBuffer()
                                .getSquareSumPerChannel(numOfChannel)
                                .mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                            numOfSample += buffer.remaining() / (numOfChannel * Float.SIZE_BYTES)
                        }

                    else -> throw IOException("Byte format $byteFormat is not supported")
                }
            }
        } else if (enablePeak) {
            synchronized(peakSync) {
                maxPeakValue = when (byteFormat) {
                    AudioFormat.ENCODING_PCM_8BIT -> buffer.getMaxValuePerChannel(numOfChannel)
                        .mapIndexed { index, value ->
                            value.coerceAtLeast(maxPeakValue[index] as Byte)
                        }.toMutableList()
                    AudioFormat.ENCODING_PCM_16BIT -> buffer.asShortBuffer()
                        .getMaxValuePerChannel(numOfChannel)
                        .mapIndexed { index, value ->
                            value.coerceAtLeast(maxPeakValue[index] as Short)
                        }.toMutableList()
                    AudioFormat.ENCODING_PCM_32BIT -> buffer.asIntBuffer()
                        .getMaxValuePerChannel(numOfChannel)
                        .mapIndexed { index, value ->
                            value.coerceAtLeast(maxPeakValue[index] as Int)
                        }.toMutableList()
                    AudioFormat.ENCODING_PCM_FLOAT -> buffer.asFloatBuffer()
                        .getMaxValuePerChannel(numOfChannel)
                        .mapIndexed { index, value ->
                            value.coerceAtLeast(maxPeakValue[index] as Float)
                        }.toMutableList()
                    else -> throw IOException("Byte format $byteFormat is not supported")
                }
            }
        } else if (enableRms) {
            synchronized(rmsSync) {
                when (byteFormat) {
                    AudioFormat.ENCODING_PCM_8BIT -> {
                        squareSum = buffer
                            .getSquareSumPerChannel(numOfChannel)
                            .mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                        numOfSample += buffer.remaining() / numOfChannel
                    }
                    AudioFormat.ENCODING_PCM_16BIT -> {
                        squareSum = buffer.asShortBuffer()
                            .getSquareSumPerChannel(numOfChannel)
                            .mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                        numOfSample += buffer.remaining() / (numOfChannel * Short.SIZE_BYTES)
                    }
                    AudioFormat.ENCODING_PCM_32BIT -> {
                        squareSum = buffer.asIntBuffer()
                            .getSquareSumPerChannel(numOfChannel)
                            .mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                        numOfSample += buffer.remaining() / (numOfChannel * Int.SIZE_BYTES)
                    }
                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        squareSum = buffer.asFloatBuffer()
                            .getSquareSumPerChannel(numOfChannel)
                            .mapIndexed { index, fl -> fl + squareSum[index] }.toMutableList()
                        numOfSample += buffer.remaining() / (numOfChannel * Float.SIZE_BYTES)
                    }
                    else -> throw IOException("Byte format $byteFormat is not supported")
                }
            }
        }
    }
}