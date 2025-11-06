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
import io.github.komedia.komuxer.flv.config.AudioFourCC
import io.github.komedia.komuxer.flv.config.SoundSize
import io.github.komedia.komuxer.flv.tags.audio.AACAudioDataFactory
import io.github.komedia.komuxer.flv.tags.audio.AudioData
import io.github.komedia.komuxer.flv.tags.audio.ExtendedAudioDataFactory
import io.github.komedia.komuxer.flv.tags.audio.codedFrame
import io.github.komedia.komuxer.flv.tags.audio.sequenceStart
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.utils.av.audio.opus.OpusCsdParser
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.AudioFlvMuxerInfo


internal interface IFlvAudioDataFactory {
    fun create(
        frame: Frame, withSequenceHeader: Boolean = false
    ): List<AudioData>

    companion object {
        fun createFactory(codecConfig: AudioCodecConfig): IFlvAudioDataFactory {
            return if (AudioFlvMuxerInfo.isLegacyConfig(codecConfig)
            ) {
                if (AudioCodecConfig.isAacMimeType(codecConfig.mimeType)) {
                    FlvAacAudioDataFactory(codecConfig)
                } else {
                    throw IllegalArgumentException("Audio codec is not implemented in legacy FLV: ${codecConfig.mimeType}")
                }
            } else {
                FlvExtendedAudioDataFactory()
            }
        }
    }
}

/**
 * Creates a FLV audio data factory for AAC audio.
 */
internal fun FlvAacAudioDataFactory(
    codecConfig: AudioCodecConfig
) = FlvAacAudioDataFactory(
    FlvUtils.soundSizeFromByteFormat(codecConfig.byteFormat)
)

/**
 * Creates a FLV audio data factory for AAC audio.
 */
internal fun FlvAacAudioDataFactory(
    soundSize: SoundSize
) = FlvAacAudioDataFactory(
    AACAudioDataFactory(
        soundSize,
    )
)

/**
 * Internal factory to create FLV AAC audio data from StreamPack [Frame]s.
 */
internal class FlvAacAudioDataFactory(private val aacAudioDataFactory: AACAudioDataFactory) :
    IFlvAudioDataFactory {
    /**
     * Create FLV video data from a [Frame].
     */
    override fun create(
        frame: Frame, withSequenceHeader: Boolean
    ): List<AudioData> {
        val flvDatas = mutableListOf<AudioData>()

        if (withSequenceHeader) {
            val decoderConfigurationRecordBuffer = frame.extra!![0]
            flvDatas.add(
                aacAudioDataFactory.sequenceStart(
                    decoderConfigurationRecordBuffer
                )
            )
        }
        flvDatas.add(
            aacAudioDataFactory.codedFrame(frame.buffer)
        )
        return flvDatas
    }
}

/**
 * Internal factory to create extended audio FLV data from StreamPack [Frame]s.
 */
internal class FlvExtendedAudioDataFactory : IFlvAudioDataFactory {
    private fun getAacAudioDataFactory(): ExtendedAudioDataFactory {
        return aacDataFactory ?: ExtendedAudioDataFactory(AudioFourCC.AAC).also {
            aacDataFactory = it
        }
    }

    private fun createAacData(frame: Frame, withSequenceHeader: Boolean): List<AudioData> {
        val flvDatas = mutableListOf<AudioData>()
        val dataFactory = getAacAudioDataFactory()

        if (withSequenceHeader) {
            val decoderConfigurationRecordBuffer = frame.extra!![0]
            flvDatas.add(
                dataFactory.sequenceStart(
                    decoderConfigurationRecordBuffer
                )
            )
        }

        flvDatas.add(
            dataFactory.codedFrame(
                frame.buffer
            )
        )
        return flvDatas
    }

    private fun getOpusAudioDataFactory(): ExtendedAudioDataFactory {
        return opusDataFactory ?: ExtendedAudioDataFactory(AudioFourCC.OPUS).also {
            opusDataFactory = it
        }
    }

    private fun createOpusData(frame: Frame, withSequenceHeader: Boolean): List<AudioData> {
        val flvDatas = mutableListOf<AudioData>()
        val dataFactory = getOpusAudioDataFactory()

        if (withSequenceHeader) {
            frame.extra?.let {
                val identificationHeader = OpusCsdParser.findIdentificationHeader(it[0])
                if (identificationHeader != null) {
                    flvDatas.add(
                        dataFactory.sequenceStart(identificationHeader)
                    )
                }
            }
        }

        flvDatas.add(
            dataFactory.codedFrame(
                frame.buffer
            )
        )
        return flvDatas
    }

    override fun create(frame: Frame, withSequenceHeader: Boolean): List<AudioData> {
        // Currently only AVC is supported
        return when {
            frame.mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS -> createOpusData(
                frame,
                withSequenceHeader
            )

            AudioCodecConfig.isAacMimeType(frame.mimeType) -> createAacData(
                frame,
                withSequenceHeader
            )

            else -> throw IllegalArgumentException("Unsupported audio mime type: ${frame.mimeType}")
        }
    }

    companion object {
        private var opusDataFactory: ExtendedAudioDataFactory? = null
        private var aacDataFactory: ExtendedAudioDataFactory? = null
    }
}