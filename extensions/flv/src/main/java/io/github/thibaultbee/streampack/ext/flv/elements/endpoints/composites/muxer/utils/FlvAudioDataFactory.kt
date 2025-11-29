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
import io.github.thibaultbee.streampack.ext.flv.elements.endpoints.composites.muxer.utils.FlvAudioDataFactory.FlvAacAudioDataFactory
import java.nio.ByteBuffer


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

internal class FlvAudioDataFactory {
    internal interface IAudioDataFactory {
        fun create(
            frame: Frame, withSequenceStart: Boolean = false
        ): List<AudioData>
    }

    /**
     * Internal factory to create FLV AAC audio data from StreamPack [Frame]s.
     */
    internal class FlvAacAudioDataFactory(private val aacAudioDataFactory: AACAudioDataFactory) :
        IAudioDataFactory {
        /**
         * Create FLV video data from a [Frame].
         */
        override fun create(
            frame: Frame, withSequenceStart: Boolean
        ): List<AudioData> {
            val flvDatas = mutableListOf<AudioData>()

            if (withSequenceStart) {
                val decoderConfigurationRecordBuffer = frame.extra!![0]
                flvDatas.add(
                    aacAudioDataFactory.sequenceStart(
                        decoderConfigurationRecordBuffer
                    )
                )
            }
            flvDatas.add(
                aacAudioDataFactory.codedFrame(frame.rawBuffer)
            )
            return flvDatas
        }
    }

    /**
     * Internal factory to create extended audio FLV data from StreamPack [Frame]s.
     */
    internal class FlvExtendedAudioDataFactory(
        private val factory: ExtendedAudioDataFactory,
        private val onSequenceStart: (Frame) -> ByteBuffer?,
    ) : IAudioDataFactory {
        override fun create(frame: Frame, withSequenceStart: Boolean): List<AudioData> {
            val flvDatas = mutableListOf<AudioData>()

            if (withSequenceStart) {
                val sequenceStart = onSequenceStart(frame)
                sequenceStart?.let {
                    flvDatas.add(
                        factory.sequenceStart(
                            it
                        )
                    )
                }
            }

            flvDatas.add(
                factory.codedFrame(
                    frame.rawBuffer
                )
            )
            return flvDatas
        }
    }

    companion object {
        private var aacLegacyDataFactory: IAudioDataFactory? = null
        private var aacExtendedDataFactory: IAudioDataFactory? = null
        private var opusDataFactory: IAudioDataFactory? = null

        private fun getLegacyAacAudioDataFactory(codecConfig: AudioCodecConfig): IAudioDataFactory {
            return aacLegacyDataFactory ?: FlvAacAudioDataFactory(codecConfig).also {
                aacLegacyDataFactory = it
            }
        }

        private fun getExtendedAacAudioDataFactory(): IAudioDataFactory {
            return aacExtendedDataFactory ?: createExtendedAacFactory().also {
                aacExtendedDataFactory = it
            }
        }

        private fun getOpusAudioDataFactory(): IAudioDataFactory {
            return opusDataFactory ?: createOpusFactory().also {
                opusDataFactory = it
            }
        }

        fun createFactory(codecConfig: AudioCodecConfig): IAudioDataFactory {
            return when {
                AudioCodecConfig.isAacMimeType(codecConfig.mimeType) && AudioFlvMuxerInfo.isLegacyConfig(
                    codecConfig
                ) -> getLegacyAacAudioDataFactory(codecConfig)

                AudioCodecConfig.isAacMimeType(codecConfig.mimeType) -> getExtendedAacAudioDataFactory()
                codecConfig.mimeType == MediaFormat.MIMETYPE_AUDIO_OPUS -> createOpusFactory()
                else -> throw IllegalArgumentException("Audio codec is not implemented in FLV: ${codecConfig.mimeType}")
            }
        }

        private fun createExtendedAacFactory(): IAudioDataFactory {
            return FlvExtendedAudioDataFactory(
                ExtendedAudioDataFactory(AudioFourCC.AAC),
                onSequenceStart = { frame ->
                    frame.extra!![0]
                }
            )
        }

        private fun createOpusFactory(): IAudioDataFactory {
            return FlvExtendedAudioDataFactory(
                ExtendedAudioDataFactory(AudioFourCC.OPUS),
                onSequenceStart = { frame ->
                    frame.extra?.let {
                        OpusCsdParser.findIdentificationHeader(it[0])
                    }
                }
            )
        }
    }
}