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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.packets

import android.media.MediaCodecInfo
import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxer
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.ITSElement
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.Service
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.Stream
import io.github.thibaultbee.streampack.core.internal.utils.extensions.put
import io.github.thibaultbee.streampack.core.internal.utils.extensions.putShort
import java.nio.ByteBuffer

class Pmt(
    listener: io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.IMuxer.IMuxerListener? = null,
    private val service: Service,
    var streams: List<Stream>,
    pid: Short,
    versionNumber: Byte = 0,
) : Psi(
    listener,
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
        get() = 32 + 40 * streams.size + 8 * programInfoLength
    override val size: Int
        get() = bitSize / Byte.SIZE_BITS

    private val programInfoLength: Int
        get() = getProgramInfoLength(MediaFormat.MIMETYPE_AUDIO_OPUS) * streams.filter { it.config.mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS }.size + getProgramInfoLength(
            MediaFormat.MIMETYPE_VIDEO_HEVC
        ) * streams.filter { it.config.mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC }.size

    fun write() {
        if (service.pcrPid != null) {
            write(toByteBuffer())
        }
    }

    override fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)

        buffer.putShort(
            (0b111 shl 13)  // Reserved
                    or service.pcrPid!!.toInt()
        )

        buffer.putShort(0b1111 shl 12) // Reserved + First two bits of program_info_length shall be '00' + program_info_length

        streams.forEach {
            buffer.put(StreamType.fromMimeType(it.config.mimeType, it.config.profile).value)
            buffer.putShort(
                (0b111 shl 13) // Reserved
                        or (it.pid.toInt())
            )
            buffer.putShort(
                (0b1111 shl 12) // Reserved
                        or getProgramInfoLength(it.config.mimeType)
            ) // First two bits of ES_info_length shall be '00' + ES_info_length

            // Registration descriptor
            if (it.config.mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS) {
                putRegistrationDescriptor(buffer, "Opus")
                buffer.put(0x7F)
                buffer.put(0x02)
                buffer.put(0x80.toByte())
                buffer.put(AudioConfig.getNumberOfChannels((it.config as AudioConfig).channelConfig))
            } else if (it.config.mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                putRegistrationDescriptor(buffer, "HEVC")
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun getProgramInfoLength(mimeType: String): Int {
        return when (mimeType) {
            MediaFormat.MIMETYPE_AUDIO_OPUS -> 10
            MediaFormat.MIMETYPE_VIDEO_HEVC -> 6
            else -> 0
        }
    }

    private fun putRegistrationDescriptor(buffer: ByteBuffer, tag: String) {
        require(tag.length == 4) { "Tag must be 4 characters long" }
        buffer.put(Descriptor.REGISTRATION.value)
        buffer.put(4)
        tag.forEach {
            buffer.put(it.code)
        }
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
            fun fromMimeType(mimeType: String, profile: Int) = when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_MPEG2 -> VIDEO_MPEG2
                MediaFormat.MIMETYPE_AUDIO_MPEG -> AUDIO_MPEG1
                MediaFormat.MIMETYPE_AUDIO_AAC -> {
                    if (profile == MediaCodecInfo.CodecProfileLevel.AACObjectLC) {
                        AUDIO_AAC
                    } else {
                        AUDIO_AAC_LATM
                    }
                }
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

    enum class Descriptor(val value: Byte) {
        VIDEO_STREAM(0x02),
        REGISTRATION(0x05),
        ISO_639_LANGUAGE(0x0a),
        IOD(0x1d),
        SL(0x1e),
        FMC(0x1f),
        METADATA(0x26),
        METADATA_STD(0x27)
    }
}