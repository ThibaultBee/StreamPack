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

import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.script.Metadata
import io.github.thibaultbee.krtmp.logger.KrtmpLogger
import io.github.thibaultbee.streampack.core.elements.data.Frame
import io.github.thibaultbee.streampack.core.elements.encoders.AudioCodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.encoders.VideoCodecConfig

/**
 * Handles FLV streams and creates FLV data from frames.
 *
 * Supports one audio and one video stream.
 *
 * Internal FLV frames handler for FLV based processing (RTMP and FLV files).
 */
class FlvDataBuilder {
    private var audioStream: AudioFlvStream? = null
    private var videoStream: VideoFlvStream? = null

    val hasAudio: Boolean
        get() = audioStream != null

    val hasVideo: Boolean
        get() = videoStream != null

    /**
     * FLV metadata containing information about the audio and video streams.
     */
    val metadata: Metadata
        get() = Metadata(audioStream?.flvConfig, videoStream?.flvConfig)

    /**
     * Adds multiple streams to the filter.
     *
     * @param streamConfigs The list of stream configurations.
     * @return A map of stream configurations to their corresponding stream PIDs.
     */
    fun addStreams(streamConfigs: List<CodecConfig>): Map<CodecConfig, Int> {
        return streamConfigs.associateWith { addStream(it) }
    }

    /**
     * Adds a stream to the filter.
     *
     * @param streamConfig The stream configuration.
     * @return The stream PID. 0 for audio, 1 for video.
     */
    fun addStream(streamConfig: CodecConfig): Int {
        return when (streamConfig) {
            is AudioCodecConfig -> {
                if (audioStream != null) {
                    throw IllegalStateException("Audio stream already added")
                }
                audioStream = AudioFlvStream(streamConfig)
                AUDIO_STREAM_PID
            }

            is VideoCodecConfig -> {
                if (videoStream != null) {
                    throw IllegalStateException("Video stream already added")
                }
                videoStream = VideoFlvStream(streamConfig)
                VIDEO_STREAM_PID
            }
        }
    }

    fun clearStreams() {
        audioStream = null
        videoStream = null
    }

    fun write(
        frame: Frame,
        streamPid: Int
    ): List<FLVData> {
        return when (streamPid) {
            AUDIO_STREAM_PID -> audioStream?.create(frame)
                ?: throw IllegalStateException("Audio stream not added")

            VIDEO_STREAM_PID -> videoStream?.create(frame)
                ?: throw IllegalStateException("Video stream not added")

            else -> throw IllegalArgumentException("Invalid stream PID $streamPid for frame $frame")
        }
    }

    companion object Companion {
        private const val AUDIO_STREAM_PID = 0
        private const val VIDEO_STREAM_PID = 1

        init {
            KrtmpLogger.logger = StreamPackLogger()
        }
    }
}
