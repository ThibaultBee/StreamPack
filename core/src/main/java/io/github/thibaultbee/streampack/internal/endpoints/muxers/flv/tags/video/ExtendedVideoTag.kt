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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.tags.video

import android.media.MediaFormat
import android.os.Build
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.tags.FlvTag
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.tags.TagType
import io.github.thibaultbee.streampack.internal.utils.av.FourCCs
import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt24
import java.nio.ByteBuffer

class ExtendedVideoTag(
    pts: Long,
    private val buffer: ByteBufferWriter,
    private val isKeyFrame: Boolean,
    private val packetType: PacketType,
    private val mimeType: String
) :
    FlvTag(pts, TagType.VIDEO) {

    init {
        require(isSupportedCodec(mimeType)) {
            "Only AV1, VP9 and HEVC are supported"
        }
    }

    override fun writeTagHeader(output: ByteBuffer) {
        val frameType = if (isKeyFrame) {
            FrameType.KEY
        } else {
            FrameType.INTER
        }

        // ExVideoTagHeader
        output.put(
            0x80 or // IsExHeader
                    (frameType.value shl 4) or // Frame Type
                    packetType.value // PacketType
        )
        output.putInt(FourCCs.fromMimeType(mimeType).value.code) // Video FourCC
    }

    override val tagHeaderSize = VIDEO_TAG_HEADER_SIZE

    override fun writeBody(output: ByteBuffer) {
        when (packetType) {
            PacketType.META_DATA -> {
                throw NotImplementedError("PacketType $packetType is not supported for $mimeType")
            }

            PacketType.SEQUENCE_END -> {
                // signals end of sequence
            }

            else -> {
                if ((packetType == PacketType.CODED_FRAMES) && (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                    output.putInt24(0) // TODO: CompositionTime
                }
                buffer.write(output)
            }
        }
    }

    override val bodySize = computeBodySize()

    private fun computeBodySize(): Int {
        return when (packetType) {
            PacketType.META_DATA -> {
                throw NotImplementedError("PacketType $packetType is not supported for $mimeType")
            }

            PacketType.SEQUENCE_END -> {
                0
            }

            else -> {
                val size =
                    if ((packetType == PacketType.CODED_FRAMES) && (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                        3 // TODO: CompositionTime
                    } else {
                        0
                    }
                return buffer.size + size
            }
        }
    }

    companion object {
        private const val VIDEO_TAG_HEADER_SIZE = 5

        fun isSupportedCodec(mimeType: String): Boolean {
            val isAV1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (mimeType == MediaFormat.MIMETYPE_VIDEO_AV1)
            } else {
                false
            }
            return (isAV1) || (mimeType == MediaFormat.MIMETYPE_VIDEO_VP9) || (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)
        }
    }
}

enum class PacketType(val value: Int) {
    SEQUENCE_START(0), // Sequence Start
    CODED_FRAMES(1),
    SEQUENCE_END(2),
    CODED_FRAMES_X(3),
    META_DATA(4),
    MPEG2_TS_SEQUENCE_START(5);

    val avcPacketType: AVCPacketType
        get() {
            return if (this == SEQUENCE_START) {
                AVCPacketType.SEQUENCE_HEADER
            } else if ((this == CODED_FRAMES) || (this == CODED_FRAMES_X)) {
                AVCPacketType.NALU
            } else if (this == SEQUENCE_END) {
                AVCPacketType.END_OF_SEQUENCE
            } else {
                throw UnsupportedOperationException("Unsupported type $this for AVCPacketType")
            }
        }
}
