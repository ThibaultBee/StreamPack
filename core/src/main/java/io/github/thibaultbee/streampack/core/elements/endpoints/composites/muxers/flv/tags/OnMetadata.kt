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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags

import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.amf.containers.AmfContainer
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.amf.containers.AmfEcmaArray
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags.video.CodecID
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags.video.ExtendedVideoTag
import io.github.thibaultbee.streampack.core.elements.utils.av.FourCCs
import io.github.thibaultbee.streampack.core.logger.Logger
import java.io.IOException
import java.nio.ByteBuffer

class OnMetadata(
    streams: List<Metadata>,
    duration: Double = 0.0
) :
    FlvTag(0, TagType.SCRIPT) {
    private val amfContainer = AmfContainer()

    init {
        amfContainer.add("onMetaData")
        val ecmaArray = AmfEcmaArray()
        ecmaArray.add("duration", duration)
        streams.forEach {
            when (it) {
                is AudioMetadata -> {
                    ecmaArray.add(
                        "audiocodecid",
                        it.codecID.value.toDouble()
                    )
                    ecmaArray.add("audiodatarate", it.dataRate.toDouble() / 1000) // to Kbps
                    ecmaArray.add("audiosamplerate", it.sampleRate.toDouble())
                    ecmaArray.add(
                        "audiosamplesize",
                        it.sampleSize.toDouble()
                    )
                    ecmaArray.add(
                        "stereo",
                        it.isStereo
                    )

                }

                is VideoMetadata -> {
                    it.codecID?.let { codecId ->
                        ecmaArray.add(
                            "videocodecid",
                            codecId.toDouble()
                        )
                    }
                    ecmaArray.add("videodatarate", it.dataRate.toDouble() / 1000) // to Kbps
                    ecmaArray.add("width", it.width.toDouble())
                    ecmaArray.add("height", it.height.toDouble())
                    ecmaArray.add("framerate", it.frameRate.toDouble())
                }
            }
        }
        amfContainer.add(ecmaArray)
    }

    override fun writeTagHeader(output: ByteBuffer) {
        // Do nothing
    }

    override val tagHeaderSize: Int
        get() = 0

    override fun writeBody(output: ByteBuffer) {
        amfContainer.encode(output)
    }

    override val bodySize: Int
        get() = amfContainer.size

    companion object {
        fun fromConfigs(
            configs: List<CodecConfig>
        ): OnMetadata {
            return OnMetadata(configs.map { Metadata.fromConfig(it) })
        }
    }
}

abstract class Metadata(val dataRate: Int) {
    companion object {
        fun fromConfig(
            config: CodecConfig,
        ): Metadata {
            return when (config) {
                is AudioCodecConfig -> AudioMetadata.fromAudioConfig(config)
                is VideoCodecConfig -> VideoMetadata.fromVideoConfig(config)
                else -> throw IOException("Not supported mime type: ${config.mimeType}")
            }
        }
    }
}

class AudioMetadata(
    val codecID: SoundFormat,
    dataRate: Int,
    val sampleRate: Int,
    val sampleSize: Int,
    val isStereo: Boolean
) : Metadata(dataRate) {
    companion object {
        fun fromAudioConfig(config: AudioCodecConfig): AudioMetadata {
            return AudioMetadata(
                SoundFormat.fromMimeType(config.mimeType),
                config.startBitrate,
                config.sampleRate,
                AudioCodecConfig.getNumOfBytesPerSample(config.byteFormat) * Byte.SIZE_BITS,
                AudioCodecConfig.getNumberOfChannels(config.channelConfig) == 2
            )
        }
    }
}

class VideoMetadata(
    val codecID: Int?,
    dataRate: Int,
    val width: Int,
    val height: Int,
    val frameRate: Int
) : Metadata(dataRate) {
    companion object {
        private const val TAG = "VideoMetadata"

        fun fromVideoConfig(
            config: VideoCodecConfig,
        ): VideoMetadata {
            val videoCodecID = if (ExtendedVideoTag.isSupportedCodec(config.mimeType)) {
                FourCCs.fromMimeType(config.mimeType).value.code
            } else {
                try {
                    CodecID.fromMimeType(config.mimeType).value
                } catch (t: Throwable) {
                    Logger.e(TAG, "Failed to get videocodecid for: ${config.mimeType}")
                    null
                }
            }

            return VideoMetadata(
                videoCodecID,
                config.startBitrate,
                config.resolution.width,
                config.resolution.height,
                config.fps
            )
        }
    }
}