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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes

import io.github.thibaultbee.streampack.internal.utils.av.audio.opus.IdentificationHeader
import java.nio.ByteBuffer

class OpusSpecificBox(
    private val version: Byte = 0,
    private val outputChannelCount: Byte,
    private val preSkip: Short,
    private val inputSampleRate: Int,
    private val outputGain: Short,
    private val channelMappingFamily: Byte,
    private val channelMapping: IdentificationHeader.ChannelMapping? = null
) : Box("dOps") {
    override val size: Int = super.size + 11 + (channelMapping?.size ?: 0)

    init {
        if (channelMappingFamily != 0.toByte()) {
            require(channelMapping != null) { "Channel mapping is required when channel mapping family is not 0" }
        }
    }

    override fun write(output: ByteBuffer) {
        super.write(output)
        output.put(version)
        output.put(outputChannelCount)
        output.putShort(preSkip)
        output.putInt(inputSampleRate)
        output.putShort(outputGain)
        output.put(channelMappingFamily)
        if (channelMappingFamily != 0.toByte()) {
            channelMapping!!.write(output)
        }
    }
}