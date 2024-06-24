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

import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import java.nio.ByteBuffer

// Prefix data frame with ADTS
class ADTSFrameWriter(private val frameBuffer: ByteBuffer, private val adts: ADTS) :
    ByteBufferWriter() {
    override val size = frameBuffer.remaining() + adts.size

    override fun write(output: ByteBuffer) {
        adts.write(output)
        output.put(frameBuffer)
    }

    companion object {
        fun fromAudioConfig(frameBuffer: ByteBuffer, audioConfig: AudioConfig): ADTSFrameWriter {
            return ADTSFrameWriter(
                frameBuffer,
                ADTS.fromAudioConfig(audioConfig, frameBuffer.remaining())
            )
        }
    }
}

// Prefix data frame with AudioMuxElement
class LATMFrameWriter(
    private val audioMuxElement: AudioMuxElement
) : ByteBufferWriter() {
    override val size = 3 + audioMuxElement.size

    override fun write(output: ByteBuffer) {
        val audioMuxLengthBytes = audioMuxElement.size
        output.put(0x56)
        output.put(0xe0 or ((audioMuxLengthBytes shr 8) and 0x1F))
        output.put(audioMuxLengthBytes and 0xFF)
        audioMuxElement.write(output)
    }

    companion object {
        fun fromDecoderSpecificInfo(frameBuffer: ByteBuffer, decoderSpecificInfo: ByteBuffer): LATMFrameWriter {
            return LATMFrameWriter(
                AudioMuxElement.fromDecoderSpecificInfo(frameBuffer, decoderSpecificInfo)
            )
        }
    }
}