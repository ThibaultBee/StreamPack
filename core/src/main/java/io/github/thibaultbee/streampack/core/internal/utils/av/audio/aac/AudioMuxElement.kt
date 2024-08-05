/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.aac

import io.github.thibaultbee.streampack.core.internal.utils.av.audio.AudioSpecificConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBuffer
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.BitBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import java.nio.ByteBuffer

class AudioMuxElement(
    val muxConfigPresent: Boolean = true,
    var useSameStreamMuxConfig: Boolean,
    private val streamMuxConfig: StreamMuxConfig? = null,
    private val payload: ByteBuffer? = null
) : ByteBufferWriter() {
    private val payloadLengthInfo = PayloadLengthInfo()
    private val payloadMux = PayloadMux()

    override val size = (bitSize + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS
    private val bitSize: Int
        get() = 1 + if (!useSameStreamMuxConfig) {
            streamMuxConfig!!.bitSize
        } else {
            0
        } + payloadLengthInfo.bitSize + payloadMux.bitSize

    init {
        if (muxConfigPresent) {
            if (useSameStreamMuxConfig) {
                requireNotNull(streamMuxConfig) { "streamMuxConfig must not be null" }
            }
        }
        if (streamMuxConfig?.audioMuxVersionA == 0) {
            requireNotNull(payload)
        }
    }

    override fun write(output: ByteBuffer) {
        val writer = BitBuffer(output)

        if (muxConfigPresent) {
            writer.put(useSameStreamMuxConfig)
            if (!useSameStreamMuxConfig) {
                streamMuxConfig!!.write(writer)
            }
        }

        if (streamMuxConfig?.audioMuxVersionA == 0) {
            // PayloadLengthInfo
            payloadLengthInfo.write(writer)

            // PayloadMux
            payloadMux.write(writer)
        }
    }

    private inner class PayloadLengthInfo : BitBufferWriter() {
        private val payloadSize = payload!!.remaining()
        override val bitSize = ((payloadSize / 255) + 1) * Byte.SIZE_BITS

        override fun write(output: BitBuffer) {
            if (streamMuxConfig!!.allStreamsSameTimeFraming == true) {
                var remainingPayloadLength = payloadSize
                for (i in 0..(payloadSize - 255) step 255) {
                    output.put(0xFF, 8)
                    remainingPayloadLength -= 255
                }
                output.put(remainingPayloadLength, 8)
            } else {
                throw NotImplementedError("Not implemented yet")
            }
        }
    }

    private inner class PayloadMux : BitBufferWriter() {
        override val bitSize = payload!!.remaining() * Byte.SIZE_BITS

        override fun write(output: BitBuffer) {
            output.put(payload!!)
        }
    }

    companion object {
        fun parse(buffer: ByteBuffer): AudioMuxElement {
            val reader = BitBuffer(buffer)
            val useSameStreamMuxConfig = reader.getBoolean()
            var streamMuxConfig: StreamMuxConfig? = null
            if (!useSameStreamMuxConfig) {
                streamMuxConfig = StreamMuxConfig.parse(reader)
            }
            var payloadLengthInfo: PayloadLengthInfo? = null
            var payload: ByteBuffer? = null
            if (streamMuxConfig?.audioMuxVersionA == 0) {
                throw NotImplementedError("Not implemented yet")
            }

            return AudioMuxElement(
                true,
                useSameStreamMuxConfig,
                streamMuxConfig,
                payload
            )
        }

        fun fromDecoderSpecificInfo(
            payload: ByteBuffer,
            decoderSpecificInfo: ByteBuffer
        ): AudioMuxElement {
            return AudioMuxElement(
                muxConfigPresent = true,
                useSameStreamMuxConfig = false,
                StreamMuxConfig.fromDecoderSpecificInfo(decoderSpecificInfo),
                payload = payload
            )
        }
    }
}


class StreamMuxConfig(
    private val audioMuxVersion: Int = 0,
    val audioMuxVersionA: Int = 0,
    val allStreamsSameTimeFraming: Boolean? = null,
    private val numSubFrames: Int? = null,
    private val numProgram: Int? = null,
    private val numLayer: Int? = null,
    private val audioSpecificConfig: BitBuffer? = null, // Because it may not be byte aligned
    private val frameLengthType: Int? = null
) : BitBufferWriter() {
    override val bitSize = 1 + if (audioMuxVersion == 1) {
        1
    } else {
        0
    } + if (audioMuxVersionA == 0) {
        27 + audioSpecificConfig!!.bitEnd
    } else {
        0
    }

    init {
        if (audioMuxVersionA == 0) {
            require(allStreamsSameTimeFraming != null) { "allStreamsSameTimeFraming must be set" }
            require(numSubFrames != null) { "numSubFrames must be set" }
            require(numProgram != null) { "numProgram must be set" }
            require(numLayer != null) { "numLayer must be set" }
            require(audioSpecificConfig != null) { "audioSpecificConfig must be set" }
            require(frameLengthType != null) { "frameLengthType must be set" }
        }
    }

    override fun write(output: BitBuffer) {
        output.put(audioMuxVersion, 1)
        if (audioMuxVersion == 1) {
            output.put(audioMuxVersionA, 1)
        }
        if (audioMuxVersionA == 0) {
            output.put(allStreamsSameTimeFraming!!)
            output.put(numSubFrames!!, 6)
            output.put(numProgram!!, 4) // numProgram
            output.put(numLayer!!, 3) // numLayer

            output.put(audioSpecificConfig!!)

            output.put(frameLengthType!!, 3)
            output.put(0xFF, 8) // latmBufferFullness
            output.put(0, 1) // otherDataPresent
            output.put(0, 1) // crcCheckPresent
        }
    }

    companion object {
        fun parse(buffer: ByteBuffer): StreamMuxConfig {
            val bitBuffer = BitBuffer(buffer)
            return parse(bitBuffer)
        }

        fun parse(bitBuffer: BitBuffer): StreamMuxConfig {
            val audioMuxVersion = bitBuffer.getInt(1)
            val audioMuxVersionA = if (audioMuxVersion == 1) {
                bitBuffer.getInt(1)
            } else {
                0
            }

            var allStreamsSameTimeFraming: Boolean? = null
            var numSubFrames: Int? = null
            var numProgram: Int? = null
            var audioSpecificConfigs = mutableListOf<AudioSpecificConfig>()
            var frameLengthTypes = mutableListOf<Int>()
            if (audioMuxVersionA == 0) {
                if (audioMuxVersion == 1) {
                    throw NotImplementedError("audioMuxVersion == 1 is not implemented yet")
                }
                allStreamsSameTimeFraming = bitBuffer.getBoolean()
                numSubFrames = bitBuffer.getInt(6)
                numProgram = bitBuffer.getInt(4)
                for (i in 0..numProgram) {
                    val numLayer = bitBuffer.getInt(3)
                    for (j in 0..numLayer) {
                        val useSameConfig = if ((i == 0) && (j == 0)) {
                            false
                        } else {
                            bitBuffer.getBoolean()
                        }
                        if (!useSameConfig) {
                            if (audioMuxVersion == 0) {
                                audioSpecificConfigs.add(AudioSpecificConfig.parse(bitBuffer))
                            } else {
                                throw NotImplementedError("audioMuxVersion == 1 is not implemented yet")
                            }
                        }
                        val frameLengthType = bitBuffer.getInt(3)
                        frameLengthTypes.add(frameLengthType)
                        when (frameLengthType) {
                            0 -> bitBuffer.getLong(8) // latmBufferFullness
                            else -> {
                                throw NotImplementedError("frameLengthType $frameLengthType is not implemented yet")
                            }
                        }
                        val otherDataPresent = bitBuffer.getBoolean()
                        if (otherDataPresent) {
                            throw NotImplementedError("otherDataPresent is not implemented yet")
                        }
                        val crcCheckPresent = bitBuffer.getBoolean()
                        if (crcCheckPresent) {
                            throw NotImplementedError("crcCheckPresent is not implemented yet")
                        }
                    }
                }
            }

            return StreamMuxConfig(
                audioMuxVersion = audioMuxVersion,
                audioMuxVersionA = audioMuxVersionA,
                allStreamsSameTimeFraming = allStreamsSameTimeFraming,
                numSubFrames = numSubFrames,
                audioSpecificConfig = audioSpecificConfigs.firstOrNull()?.toBitBuffer(),
                frameLengthType = frameLengthTypes.firstOrNull()
            )
        }

        fun fromDecoderSpecificInfo(decoderSpecificInfo: ByteBuffer): StreamMuxConfig {
            val audioSpecificConfig = AudioSpecificConfig.parse(decoderSpecificInfo)
            decoderSpecificInfo.rewind()
            return StreamMuxConfig(
                allStreamsSameTimeFraming = true,
                numSubFrames = 0,
                numProgram = 0,
                numLayer = 0,
                frameLengthType = 0,
                audioSpecificConfig = BitBuffer(
                    decoderSpecificInfo,
                    bitEnd = audioSpecificConfig.bitSize - 1
                )
            )
        }
    }
}
