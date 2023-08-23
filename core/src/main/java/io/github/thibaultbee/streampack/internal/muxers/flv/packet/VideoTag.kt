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
package io.github.thibaultbee.streampack.internal.muxers.flv.packet

import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.av.video.getStartCodeSize
import io.github.thibaultbee.streampack.internal.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.internal.utils.av.video.removeStartCode
import io.github.thibaultbee.streampack.internal.utils.extensions.put
import io.github.thibaultbee.streampack.internal.utils.extensions.putInt24
import java.io.IOException
import java.nio.ByteBuffer

class VideoTag(
    pts: Long,
    private val buffers: List<ByteBuffer>,
    private val isKeyFrame: Boolean,
    private val isSequenceHeader: Boolean = false,
    private val videoConfig: VideoConfig
) :
    FlvTag(pts, TagType.VIDEO) {
    constructor(
        pts: Long,
        buffer: ByteBuffer,
        isKeyFrame: Boolean,
        isSequenceHeader: Boolean = false,
        videoConfig: VideoConfig
    ) : this(pts, listOf(buffer), isKeyFrame, isSequenceHeader, videoConfig)

    companion object {
        private const val VIDEO_TAG_HEADER_SIZE = 1
    }

    init {
        if (videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
            if (!isSequenceHeader) {
                require(buffers.size == 1) { "Only one buffer is expected for raw frame" }
            } else {
                require(buffers.size == 2) { "Both SPS and PPS are expected in sequence mode" }
            }
        }
        if (videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
            if (!isSequenceHeader) {
                require(buffers.size == 1) { "Only one buffer is expected for raw frame" }
            } else {
                require(buffers.size == 3) { "Both SPS, PPS and VPS are expected in sequence mode" }
            }
        }
    }

    override fun writeTagHeader(buffer: ByteBuffer) {
        val frameType = if (isKeyFrame) {
            FrameType.KEY
        } else {
            FrameType.INTER
        }
        buffer.put(
            (frameType.value shl 4) or // Frame Type
                    (CodecID.fromMimeType(videoConfig.mimeType).value) // CodecId
        )
        if ((videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_AVC)
            || (videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)
        ) {
            if (isSequenceHeader) {
                buffer.put(AVCPacketType.SEQUENCE.value) // AVC sequence header
            } else {
                buffer.put(AVCPacketType.NALU.value) // AVC NALU
            }
            buffer.putInt24(0) // TODO: CompositionTime
        }
    }

    override val tagHeaderSize = computeHeaderSize()

    private fun computeHeaderSize(): Int {
        var size = VIDEO_TAG_HEADER_SIZE
        if ((videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_AVC)
            || (videoConfig.mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC)
        ) {
            size += 4 // AVCPacketType & CompositionTime
        }
        return size
    }

    override fun writePayload(buffer: ByteBuffer) {
        if (isSequenceHeader) {
            when (videoConfig.mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> {
                    AVCDecoderConfigurationRecord.fromParameterSets(buffers[0], buffers[1])
                        .write(buffer)
                }

                MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                    HEVCDecoderConfigurationRecord.fromParameterSets(buffers).write(buffer)
                }

                else -> {
                    throw IOException("Mimetype ${videoConfig.mimeType} is not supported")
                }
            }

        } else {
            val noStartCodeBuffer = buffers[0].removeStartCode()
            buffer.putInt(noStartCodeBuffer.remaining())
            buffer.put(noStartCodeBuffer)
        }
    }

    override val payloadSize = computePayloadSize()

    private fun computePayloadSize(): Int {
        return if (isSequenceHeader) {
            when (videoConfig.mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> {
                    AVCDecoderConfigurationRecord.getSize(buffers[0], buffers[1])
                }

                MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                    HEVCDecoderConfigurationRecord.getSize(buffers[0], buffers[1], buffers[2])
                }

                else -> {
                    throw IOException("Mimetype ${videoConfig.mimeType} is not supported")
                }
            }
        } else {
            return buffers[0].remaining() - buffers[0].getStartCodeSize() + 4 // Replace start code with annex B
        }
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
    AVC(7),
    HEVC(12); // Not standards

    fun toMimeType() = when (this) {
        SORENSON_H263 -> MediaFormat.MIMETYPE_VIDEO_H263
        AVC -> MediaFormat.MIMETYPE_VIDEO_AVC
        HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
        else -> throw IOException("MimeType is not supported: $this")
    }

    companion object {
        fun fromMimeType(mimeType: String) = when (mimeType) {
            MediaFormat.MIMETYPE_VIDEO_H263 -> SORENSON_H263
            MediaFormat.MIMETYPE_VIDEO_AVC -> AVC
            MediaFormat.MIMETYPE_VIDEO_HEVC -> HEVC
            else -> throw IOException("MimeType is not supported: $mimeType")
        }
    }
}

enum class AVCPacketType(val value: Int) {
    SEQUENCE(0),
    NALU(1),
    END_OF_SEQUENCE(2)
}