/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils

import android.media.MediaFormat
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.tags.video.AVCHEVCExtendedVideoDataFactory
import io.github.thibaultbee.krtmp.flv.tags.video.AVCVideoDataFactory
import io.github.thibaultbee.krtmp.flv.tags.video.CommonExtendedVideoDataFactory
import io.github.thibaultbee.krtmp.flv.tags.video.ExtendedVideoDataFactory
import io.github.thibaultbee.krtmp.flv.tags.video.HEVCExtendedVideoDataFactory
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.flv.tags.video.VideoFrameType
import io.github.thibaultbee.krtmp.flv.tags.video.codedFrame
import io.github.thibaultbee.krtmp.flv.tags.video.sequenceStart
import io.github.thibaultbee.krtmp.flv.utils.transformToAVCC
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.elements.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.elements.utils.av.video.vpx.VPCodecConfigurationRecord
import java.nio.ByteBuffer


/**
 * Internal factory to create FLV data from StreamPack [Frame]s.
 */
internal class FlvVideoDataFactory {

    private fun getAvcVideoDataFactory(): AVCVideoDataFactory {
        return avcDataFactory ?: AVCVideoDataFactory().also {
            avcDataFactory = it
        }
    }

    private fun createAVCData(frame: Frame, withSequenceHeader: Boolean): List<VideoData> {
        val flvDatas = mutableListOf<VideoData>()
        val videoDataFactory = getAvcVideoDataFactory()

        val videoFrameType = if (frame.isKeyFrame) {
            VideoFrameType.KEY
        } else {
            VideoFrameType.INTER
        }
        if (frame.isKeyFrame && withSequenceHeader) {
            val decoderConfigurationRecordBuffer =
                AVCDecoderConfigurationRecord.fromParameterSets(
                    frame.extra!![0],
                    frame.extra!![1]
                ).toByteBuffer()
            flvDatas.add(
                videoDataFactory.sequenceStart(
                    decoderConfigurationRecordBuffer
                )
            )
        }
        val source = transformToAVCC(frame.buffer)
        flvDatas.add(
            videoDataFactory.codedFrame(
                videoFrameType,
                source.first,
                source.second
            )
        )
        return flvDatas
    }

    /**
     * Create FLV video data from a [Frame].
     */
    private fun createExtendedData(
        factory: CommonExtendedVideoDataFactory,
        frame: Frame,
        withSequenceHeader: Boolean,
        onDecoderConfigurationRecord: () -> ByteBuffer,
    ): List<VideoData> {
        val flvDatas = mutableListOf<VideoData>()

        val videoFrameType = if (frame.isKeyFrame) {
            VideoFrameType.KEY
        } else {
            VideoFrameType.INTER
        }
        if (frame.isKeyFrame && withSequenceHeader) {
            val decoderConfigurationRecordBuffer =
                onDecoderConfigurationRecord()
            flvDatas.add(
                factory.sequenceStart(
                    decoderConfigurationRecordBuffer
                )
            )
        }
        flvDatas.add(
            when (factory) {
                is AVCHEVCExtendedVideoDataFactory -> {
                    val source = transformToAVCC(frame.buffer)
                    factory.codedFrameX(
                        videoFrameType,
                        source.first,
                        source.second
                    )
                }

                is ExtendedVideoDataFactory -> {
                    factory.codedFrame(
                        videoFrameType,
                        frame.buffer
                    )
                }
            }
        )
        return flvDatas
    }

    private fun getHevcVideoDataFactory(): HEVCExtendedVideoDataFactory {
        return hevcDataFactory ?: HEVCExtendedVideoDataFactory().also {
            hevcDataFactory = it
        }
    }

    private fun createHEVCData(frame: Frame, withSequenceHeader: Boolean) =
        createExtendedData(getHevcVideoDataFactory(), frame, withSequenceHeader) {
            HEVCDecoderConfigurationRecord.fromParameterSets(
                frame.extra!![0],
                frame.extra!![1],
                frame.extra!![2]
            ).toByteBuffer()
        }

    private fun getAv1VideoDataFactory(): ExtendedVideoDataFactory {
        return av1DataFactory ?: ExtendedVideoDataFactory(VideoFourCC.AV1).also {
            av1DataFactory = it
        }
    }

    private fun createAV1Data(frame: Frame, withSequenceHeader: Boolean) =
        createExtendedData(getAv1VideoDataFactory(), frame, withSequenceHeader) {
            // Extra is AV1CodecConfigurationRecord
            frame.extra!![0]
        }

    private fun getVp9VideoDataFactory(): ExtendedVideoDataFactory {
        return vp9DataFactory ?: ExtendedVideoDataFactory(VideoFourCC.VP9).also {
            vp9DataFactory = it
        }
    }

    private fun createVP9Data(frame: Frame, withSequenceHeader: Boolean) =
        createExtendedData(getVp9VideoDataFactory(), frame, withSequenceHeader) {
            VPCodecConfigurationRecord.fromMediaFormat(frame.format).toByteBuffer()
        }

    fun create(frame: Frame, withSequenceHeader: Boolean): List<VideoData> {
        // Currently only AVC is supported
        return when (frame.mimeType) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> createAVCData(frame, withSequenceHeader)
            MediaFormat.MIMETYPE_VIDEO_HEVC -> createHEVCData(frame, withSequenceHeader)
            MediaFormat.MIMETYPE_VIDEO_AV1 -> createAV1Data(frame, withSequenceHeader)
            MediaFormat.MIMETYPE_VIDEO_VP9 -> createVP9Data(frame, withSequenceHeader)
            else -> throw IllegalArgumentException("Unsupported video mime type: ${frame.mimeType}")
        }
    }

    companion object {
        private var avcDataFactory: AVCVideoDataFactory? = null
        private var hevcDataFactory: HEVCExtendedVideoDataFactory? = null
        private var av1DataFactory: ExtendedVideoDataFactory? = null
        private var vp9DataFactory: ExtendedVideoDataFactory? = null
    }
}
