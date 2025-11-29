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
import io.github.komedia.komuxer.avutil.util.transformToAVCC
import io.github.komedia.komuxer.flv.config.VideoFourCC
import io.github.komedia.komuxer.flv.tags.video.AVCHEVCExtendedVideoDataFactory
import io.github.komedia.komuxer.flv.tags.video.AVCVideoDataFactory
import io.github.komedia.komuxer.flv.tags.video.CommonExtendedVideoDataFactory
import io.github.komedia.komuxer.flv.tags.video.ExtendedVideoDataFactory
import io.github.komedia.komuxer.flv.tags.video.HEVCExtendedVideoDataFactory
import io.github.komedia.komuxer.flv.tags.video.VideoData
import io.github.komedia.komuxer.flv.tags.video.VideoFrameType
import io.github.komedia.komuxer.flv.tags.video.codedFrame
import io.github.komedia.komuxer.flv.tags.video.sequenceStart
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.utils.av.video.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.elements.utils.av.video.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.streampack.core.elements.utils.av.video.vpx.VPCodecConfigurationRecord
import java.nio.ByteBuffer


/**
 * Internal factory to create FLV data from StreamPack [Frame]s.
 */
internal class FlvVideoDataFactory {
    /**
     * Interface for FLV video data factory.
     */
    internal interface IVideoDataFactory {
        fun create(
            frame: Frame,
            withSequenceStart: Boolean = false
        ): List<VideoData>
    }

    /**
     * Factory to create legacy FLV AVC video data from a [Frame].
     */
    private class AVCFlvVideoDataFactory : IVideoDataFactory {
        private val factory = AVCVideoDataFactory()

        override fun create(frame: Frame, withSequenceStart: Boolean): List<VideoData> {
            val flvDatas = mutableListOf<VideoData>()

            val videoFrameType = if (frame.isKeyFrame) {
                VideoFrameType.KEY
            } else {
                VideoFrameType.INTER
            }
            if (frame.isKeyFrame && withSequenceStart) {
                val decoderConfigurationRecordBuffer =
                    AVCDecoderConfigurationRecord.fromParameterSets(
                        frame.extra!![0],
                        frame.extra!![1]
                    ).toByteBuffer()
                flvDatas.add(
                    factory.sequenceStart(
                        decoderConfigurationRecordBuffer
                    )
                )
            }
            val source = transformToAVCC(frame.rawBuffer)
            flvDatas.add(
                factory.codedFrame(
                    videoFrameType,
                    source.first,
                    source.second
                )
            )
            return flvDatas
        }
    }

    private class FlvExtendedVideoDataFactory(
        private val factory: CommonExtendedVideoDataFactory,
        private val onSequenceStart: (Frame) -> ByteBuffer,
    ) : IVideoDataFactory {
        /**
         * Create FLV video data from a [Frame].
         */
        override fun create(
            frame: Frame,
            withSequenceStart: Boolean
        ): List<VideoData> {
            val flvDatas = mutableListOf<VideoData>()

            val videoFrameType = if (frame.isKeyFrame) {
                VideoFrameType.KEY
            } else {
                VideoFrameType.INTER
            }
            factory is AVCHEVCExtendedVideoDataFactory
            if (frame.isKeyFrame && withSequenceStart) {
                val decoderConfigurationRecordBuffer =
                    onSequenceStart(frame)
                flvDatas.add(
                    factory.sequenceStart(
                        decoderConfigurationRecordBuffer
                    )
                )
            }
            flvDatas.add(
                when (factory) {
                    is AVCHEVCExtendedVideoDataFactory -> {
                        val source = transformToAVCC(frame.rawBuffer)
                        factory.codedFrameX(
                            videoFrameType,
                            source.first,
                            source.second
                        )
                    }

                    is ExtendedVideoDataFactory -> {
                        factory.codedFrame(
                            videoFrameType,
                            frame.rawBuffer
                        )
                    }
                }
            )
            return flvDatas
        }
    }

    companion object {
        private var avcDataFactory: IVideoDataFactory? = null
        private var hevcDataFactory: IVideoDataFactory? = null
        private var av1DataFactory: IVideoDataFactory? = null
        private var vp9DataFactory: IVideoDataFactory? = null

        private fun getAvcDataFactory(): IVideoDataFactory {
            return avcDataFactory ?: AVCFlvVideoDataFactory().also {
                avcDataFactory = it
            }
        }

        private fun getHevcVideoDataFactory(): IVideoDataFactory {
            return hevcDataFactory ?: createHEVCFactory().also {
                hevcDataFactory = it
            }
        }

        private fun getAv1DataFactory(): IVideoDataFactory {
            return av1DataFactory ?: createAV1Factory().also {
                av1DataFactory = it
            }
        }

        private fun getVp9DataFactory(): IVideoDataFactory {
            return vp9DataFactory ?: createVP9Factory().also {
                vp9DataFactory = it
            }
        }

        /**
         * Create a FLV video data factory for the given [codecConfig].
         */
        fun createFactory(codecConfig: VideoCodecConfig): IVideoDataFactory {
            // Currently only AVC is supported
            return when (codecConfig.mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> getAvcDataFactory()
                MediaFormat.MIMETYPE_VIDEO_HEVC -> getHevcVideoDataFactory()
                MediaFormat.MIMETYPE_VIDEO_AV1 -> getAv1DataFactory()
                MediaFormat.MIMETYPE_VIDEO_VP9 -> getVp9DataFactory()
                else -> throw IllegalArgumentException("Unsupported video mime type: ${codecConfig.mimeType}")
            }
        }

        private fun createHEVCFactory(): IVideoDataFactory {
            return FlvExtendedVideoDataFactory(HEVCExtendedVideoDataFactory()) { frame ->
                // Extra is VPS, SPS, PPS
                HEVCDecoderConfigurationRecord.fromParameterSets(
                    frame.extra!![0],
                    frame.extra!![1],
                    frame.extra!![2]
                ).toByteBuffer()
            }
        }

        private fun createAV1Factory(): IVideoDataFactory {
            return FlvExtendedVideoDataFactory(ExtendedVideoDataFactory(VideoFourCC.AV1)) { frame ->
                // Extra is AV1CodecConfigurationRecord
                frame.extra!![0]
            }
        }

        private fun createVP9Factory(): IVideoDataFactory {
            return FlvExtendedVideoDataFactory(ExtendedVideoDataFactory(VideoFourCC.VP9)) { frame ->
                VPCodecConfigurationRecord.fromMediaFormat(frame.format).toByteBuffer()
            }
        }
    }
}
