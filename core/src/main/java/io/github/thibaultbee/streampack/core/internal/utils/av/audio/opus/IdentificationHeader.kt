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
package io.github.thibaultbee.streampack.core.internal.utils.av.audio.opus

import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.extensions.getString
import io.github.thibaultbee.streampack.core.internal.utils.extensions.startWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IdentificationHeader(
    val version: Byte,
    val channelCount: Byte,
    val preSkip: Short,
    val inputSampleRate: Int,
    val outputGain: Short,
    val channelMappingFamily: Byte,
    val channelMapping: ChannelMapping?
) : ByteBufferWriter() {
    override val size = 19 + (channelMapping?.size ?: 0)

    override fun write(output: ByteBuffer) {
        throw NotImplementedError("Write Opus identification header is not implemented")
    }

    companion object {
        private const val MAGIC_SIGNATURE_SIZE = 8
        private const val MAGIC_SIGNATURE = "OpusHead"

        fun isIdentificationHeader(buffer: ByteBuffer) = buffer.startWith(MAGIC_SIGNATURE)

        fun parse(buffer: ByteBuffer): IdentificationHeader {
            val magicSignature =
                buffer.getString(MAGIC_SIGNATURE_SIZE) // Drop magic Signature: "OpusHead"
            require(magicSignature == MAGIC_SIGNATURE) { "Magic signature $MAGIC_SIGNATURE expected but $magicSignature" }
            val version = buffer.get()
            val channelCount = buffer.get()
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            val preSkip = buffer.short
            val inputSampleRate = buffer.int
            val outputGain = buffer.short
            buffer.order(ByteOrder.BIG_ENDIAN)
            val channelMappingFamily = buffer.get()
            val channelMapping = if (channelMappingFamily != 0.toByte()) {
                ChannelMapping.parse(buffer, channelCount)
            } else {
                null
            }

            return IdentificationHeader(
                version,
                channelCount,
                preSkip,
                inputSampleRate,
                outputGain,
                channelMappingFamily,
                channelMapping
            )
        }
    }

    class ChannelMapping(
        private val streamCount: Byte,
        private val coupledCount: Byte,
        private val channelMapping: ByteArray
    ) :
        ByteBufferWriter() {
        override val size: Int = 2 + channelMapping.size

        override fun write(output: ByteBuffer) {
            output.put(streamCount)
            output.put(coupledCount)
            output.put(channelMapping)
        }

        companion object {
            fun parse(buffer: ByteBuffer, outputChannelCount: Byte): ChannelMapping {
                val streamCount = buffer.get()
                val coupledCount = buffer.get()
                val channelMappingArray = ByteArray(Byte.SIZE_BYTES * outputChannelCount)
                buffer.get(channelMappingArray)

                return ChannelMapping(streamCount, coupledCount, channelMappingArray)
            }
        }
    }
}