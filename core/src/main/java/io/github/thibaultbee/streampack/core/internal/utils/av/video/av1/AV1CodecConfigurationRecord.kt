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
package io.github.thibaultbee.streampack.core.internal.utils.av.video.av1

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import io.github.thibaultbee.streampack.core.internal.utils.extensions.shl
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toInt
import java.nio.ByteBuffer

class AV1CodecConfigurationRecord(
    private val marker: Boolean = true,
    private val version: Byte = 1,
    private val seqProfile: Byte,
    private val seqLevelIdx0: Byte,
    private val seqTier0: Boolean,
    private val highBitdepth: Boolean,
    private val twelveBit: Boolean,
    private val monochrome: Boolean,
    private val chromaSubsamplingX: Boolean,
    private val chromaSubsamplingY: Boolean,
    private val chromaSamplePosition: Byte,
    private val initialPresentationDelayMinusOne: Int?,
    private val configOBUs: ByteBuffer,
) : ByteBufferWriter() {
    override val size: Int = AV1_DECODER_CONFIGURATION_RECORD_SIZE + configOBUs.remaining()

    override fun write(output: ByteBuffer) {
        output.put((marker.toInt() shl 7) or version.toInt())
        output.put((seqProfile shl 5) or seqLevelIdx0.toInt())
        output.putShort(
            (seqTier0 shl 15) or
                    (highBitdepth shl 14) or
                    (twelveBit shl 13) or
                    (monochrome shl 12) or
                    (chromaSubsamplingX shl 11) or
                    (chromaSubsamplingY shl 10) or
                    (chromaSamplePosition shl 8) or
                    if (initialPresentationDelayMinusOne != null) {
                        (0b1 shl 4) or (initialPresentationDelayMinusOne)
                    } else {
                        0
                    }
        )

        output.put(configOBUs)
    }

    companion object {
        private const val AV1_DECODER_CONFIGURATION_RECORD_SIZE = 4

        /**
         *  {max-bitrate=2000000, crop-right=719, level=32, latency=0, mime=video/av01, profile=1,
         *  bitrate=2000000, priority=0, color-standard=1, color-transfer=3,
         *  hdr10-plus-info=java.nio.HeapByteBuffer[pos=0 lim=0 cap=0], crop-bottom=1279,
         *  video-qp-average=0, crop-left=0, width=720, bitrate-mode=2, color-range=2, crop-top=0,
         *  frame-rate=30, height=1280, csd-0=java.nio.HeapByteBuffer[pos=0 lim=4 cap=4]}
         *
         */
        fun fromMediaFormat(mediaFormat: MediaFormat): AV1CodecConfigurationRecord {
            throw NotImplementedError("AV1CodecConfigurationRecord.fromMediaFormat() is not implemented")
        }
    }
}