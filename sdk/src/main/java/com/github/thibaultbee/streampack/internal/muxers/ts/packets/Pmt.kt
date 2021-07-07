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
package com.github.thibaultbee.streampack.internal.muxers.ts.packets

import android.media.MediaFormat
import com.github.thibaultbee.streampack.internal.bitbuffer.BitBuffer
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ITSElement
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Service
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Stream
import java.nio.ByteBuffer

class Pmt(
    muxerListener: IMuxerListener,
    private val service: Service,
    var streams: List<Stream>,
    pid: Short,
    versionNumber: Byte = 0,
) : Psi(
    muxerListener,
    pid,
    TID,
    true,
    false,
    service.info.id,
    versionNumber,
),
    ITSElement {
    companion object {
        // Table ids
        const val TID: Byte = 0x02
    }

    override val bitSize: Int
        get() = 32 + 40 * streams.size
    override val size: Int
        get() = bitSize / Byte.SIZE_BITS

    fun write() {
        if (service.pcrPid != null) {
            write(toByteBuffer())
        }
    }

    override fun toByteBuffer(): ByteBuffer {
        val buffer = BitBuffer.allocate(bitSize.toLong())

        buffer.put(0b111, 3) // Reserved
        buffer.put(service.pcrPid!!, 13)

        buffer.put(0b1111, 4) // Reserved
        buffer.put(0b00, 2) // First two bits of program_info_length shall be '00'
        buffer.put(0, 10) // program_info_length
        // TODO: Program Info

        streams.forEach {
            buffer.put(StreamType.fromMimeType(it.mimeType).value, 8)
            buffer.put(0b111, 3) // Reserved
            buffer.put(it.pid, 13)
            buffer.put(0b1111, 4) // Reserved

            buffer.put(0b00, 2) // First two bits of ES_info_length shall be '00'
            buffer.put(0b00, 10) // ES_info_length
            // TODO: ES Info
        }

        return buffer.toByteBuffer()
    }

    enum class StreamType(val value: Byte) {
        VIDEO_MPEG1(0x01.toByte()),
        VIDEO_MPEG2(0x02.toByte()),
        AUDIO_MPEG1(0x03.toByte()),
        AUDIO_MPEG2(0x04.toByte()),
        PRIVATE_SECTION(0x05.toByte()),
        PRIVATE_DATA(0x06.toByte()),
        AUDIO_AAC(0x0f.toByte()),
        AUDIO_AAC_LATM(0x11.toByte()),
        VIDEO_MPEG4(0x10.toByte()),
        METADATA(0x15.toByte()),
        VIDEO_H264(0x1b.toByte()),
        VIDEO_HEVC(0x24.toByte()),
        VIDEO_CAVS(0x42.toByte()),
        VIDEO_VC1(0xea.toByte()),
        VIDEO_DIRAC(0xd1.toByte()),

        AUDIO_AC3(0x81.toByte()),
        AUDIO_DTS(0x82.toByte()),
        AUDIO_TRUEHD(0x83.toByte()),
        AUDIO_EAC3(0x87.toByte());

        companion object {
            fun fromMimeType(mimeType: String) = when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_MPEG2 -> VIDEO_MPEG2
                MediaFormat.MIMETYPE_AUDIO_MPEG -> AUDIO_MPEG1
                MediaFormat.MIMETYPE_AUDIO_AAC -> AUDIO_AAC
                MediaFormat.MIMETYPE_VIDEO_MPEG4 -> VIDEO_MPEG4
                MediaFormat.MIMETYPE_VIDEO_AVC -> VIDEO_H264
                MediaFormat.MIMETYPE_VIDEO_HEVC -> VIDEO_HEVC
                MediaFormat.MIMETYPE_AUDIO_AC3 -> AUDIO_AC3
                MediaFormat.MIMETYPE_AUDIO_EAC3 -> AUDIO_EAC3
                MediaFormat.MIMETYPE_AUDIO_OPUS -> PRIVATE_DATA
                else -> PRIVATE_DATA
            }
        }
    }
}