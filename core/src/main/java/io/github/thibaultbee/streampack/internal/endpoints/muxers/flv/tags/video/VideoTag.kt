/*
 * Copyright (C) 2022 Thibault B.
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
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.tags.FlvTag
import io.github.thibaultbee.streampack.internal.endpoints.muxers.flv.tags.TagType
import io.github.thibaultbee.streampack.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt24
import java.io.IOException
import java.nio.ByteBuffer

/**
 * @param packetType 0: AVC sequence header, 1: AVC NALU, 2: AVC end of sequence. Using `PacketType` instead of `AVCPacketType` to simplify as the first 3 values are the same.
 */
class VideoTag(
    pts: Long,
    private val buffer: ByteBufferWriter,
    private val isKeyFrame: Boolean,
    private val packetType: AVCPacketType?,
    private val mimeType: String
) :
    FlvTag(pts, TagType.VIDEO) {

    private val codecID = CodecID.fromMimeType(mimeType)

    init {
        require(isSupportedCodec(mimeType)) {
            "Only H263 and H264 are supported"
        }
        if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
            requireNotNull(packetType) { "AVC packet type is required for H264" }
        }
    }

    override fun writeTagHeader(output: ByteBuffer) {
        val frameType = if (isKeyFrame) {
            FrameType.KEY
        } else {
            FrameType.INTER
        }
        output.put(
            (frameType.value shl 4) or // Frame Type
                    (codecID.value) // CodecID
        )
        if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
            output.put(packetType!!.value) // AVC sequence header or NALU
            output.putInt24(0) // TODO: CompositionTime
        }
    }

    override val tagHeaderSize = computeHeaderSize()

    private fun computeHeaderSize(): Int {
        var size = VIDEO_TAG_HEADER_SIZE
        if (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
            size += 4 // AVCPacketType & CompositionTime
        }
        return size
    }

    override fun writeBody(output: ByteBuffer) {
        when (packetType) {
            AVCPacketType.END_OF_SEQUENCE -> {
                // signals end of sequence
            }

            else -> {
                buffer.write(output)
            }
        }
    }

    override val bodySize = computeBodySize()

    private fun computeBodySize(): Int {
        return when (packetType) {
            AVCPacketType.END_OF_SEQUENCE -> {
                0
            }

            else -> {
                buffer.size
            }
        }
    }

    companion object {
        private const val VIDEO_TAG_HEADER_SIZE = 1

        fun isSupportedCodec(mimeType: String) =
            (mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) || (mimeType == MediaFormat.MIMETYPE_VIDEO_H263)
    }

}

enum class FrameType(val value: Int) {
    KEY(1),
    INTER(2),
    DISPOSABLE_INTER(3),
    GENERATED_KEY(4),
    INFO_COMMAND(5)
}

enum class CodecID(val value: Int) {
    SORENSON_H263(2),
    SCREEN_1(3),
    VP6(4),
    VP6_ALPHA(5),
    SCREEN_2(6),
    AVC(7);

    fun toMimeType() = when (this) {
        SORENSON_H263 -> MediaFormat.MIMETYPE_VIDEO_H263
        AVC -> MediaFormat.MIMETYPE_VIDEO_AVC
        else -> throw IOException("MimeType is not supported: $this")
    }

    companion object {
        fun fromMimeType(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_H263 -> SORENSON_H263
            MediaFormat.MIMETYPE_VIDEO_AVC -> AVC
            else -> throw IOException("MimeType is not supported: $mimeType")
        }
    }
}

enum class AVCPacketType(val value: Int) {
    SEQUENCE_HEADER(0),
    NALU(1),
    END_OF_SEQUENCE(2)
}
