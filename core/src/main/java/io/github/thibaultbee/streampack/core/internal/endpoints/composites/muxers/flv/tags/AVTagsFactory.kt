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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags

import android.media.MediaFormat
import io.github.thibaultbee.streampack.core.data.AudioConfig
import io.github.thibaultbee.streampack.core.data.Config
import io.github.thibaultbee.streampack.core.data.VideoConfig
import io.github.thibaultbee.streampack.core.internal.data.Frame
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.video.PacketType
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.flv.tags.video.VideoTagFactory
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.AVCCBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.ByteBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.buffer.PassthroughBufferWriter
import io.github.thibaultbee.streampack.core.internal.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.internal.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.internal.utils.av.video.vpx.VPCodecConfigurationRecord
import java.io.IOException
import java.nio.ByteBuffer

class AVTagsFactory(
    private val frame: Frame,
    private val config: Config,
    private val sendHeader: Boolean
) {
    fun build(): List<FlvTag> {
        return if (frame.isVideo) {
            createVideoTags(frame, config as VideoConfig, sendHeader)
        } else if (frame.isAudio) {
            createAudioTags(frame, config as AudioConfig, sendHeader)
        } else {
            throw IOException("Frame is neither video nor audio: ${frame.mimeType}")
        }
    }

    private fun createAudioTags(
        frame: Frame,
        config: AudioConfig,
        sendHeader: Boolean
    ): List<FlvTag> {
        val audioTag = mutableListOf<FlvTag>()
        if (sendHeader) {
            audioTag.add(
                AudioTag(
                    frame.pts,
                    frame.extra!![0],
                    if (config.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                        AACPacketType.SEQUENCE_HEADER
                    } else {
                        null
                    },
                    config
                )
            )
        }
        audioTag.add(
            AudioTag(
                frame.pts,
                frame.buffer,
                if (config.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                    AACPacketType.RAW
                } else {
                    null
                },
                config
            )
        )
        return audioTag
    }

    private fun createVideoTags(
        frame: Frame,
        config: VideoConfig,
        sendHeader: Boolean
    ): List<FlvTag> {
        val videoTags = mutableListOf<FlvTag>()
        if (frame.isKeyFrame && sendHeader) {
            videoTags.add(
                VideoTagFactory(
                    frame.pts,
                    createVideoSequenceStartBufferWriter(frame, config),
                    true,
                    PacketType.SEQUENCE_START,
                    config.mimeType
                ).build()
            )
        }

        videoTags.add(
            VideoTagFactory(
                frame.pts,
                createVideoBufferWriter(frame.buffer),
                frame.isKeyFrame,
                PacketType.CODED_FRAMES_X, // For extended codec only.
                config.mimeType
            ).build()
        )

        return videoTags
    }

    private fun createVideoSequenceStartBufferWriter(
        frame: Frame,
        config: VideoConfig
    ): ByteBufferWriter {
        return when (config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> {
                AVCDecoderConfigurationRecord.fromParameterSets(
                    frame.extra!![0],
                    frame.extra[1]
                )
            }

            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                HEVCDecoderConfigurationRecord.fromParameterSets(frame.extra!!)
            }

            MediaFormat.MIMETYPE_VIDEO_VP9 -> {
                VPCodecConfigurationRecord.fromMediaFormat(frame.format)
            }

            MediaFormat.MIMETYPE_VIDEO_AV1 -> {
                if (frame.extra != null) {
                    // Extra is AV1CodecConfigurationRecord
                    PassthroughBufferWriter(frame.extra[0])
                } else {
                    throw IOException("AV1 sequence header without CSD buffer is not supported")
                }
            }

            else -> {
                throw IOException("Unsupported video codec: ${config.mimeType}")
            }
        }
    }

    private fun createVideoBufferWriter(
        buffer: ByteBuffer
    ): ByteBufferWriter {
        return when (config.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC,
            MediaFormat.MIMETYPE_VIDEO_HEVC -> {
                AVCCBufferWriter(buffer)
            }

            else -> {
                PassthroughBufferWriter(buffer)
            }
        }
    }
}
