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
package io.github.thibaultbee.streampack.internal.muxers.flv.tags

import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.AudioConfig
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.video.PacketType
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.video.VideoTagFactory
import java.io.IOException

class AVTagsFactory(
    private val frame: Frame,
    private val config: Config
) {
    fun build(): List<FlvTag> {
        return if (frame.isVideo) {
            createVideoTags(frame, config as VideoConfig)
        } else if (frame.isAudio) {
            createAudioTags(frame, config as AudioConfig)
        } else {
            throw IOException("Frame is neither video nor audio: ${frame.mimeType}")
        }
    }

    private fun createAudioTags(
        frame: Frame,
        config: AudioConfig
    ): List<FlvTag> {
        return listOf(
            AudioTag(
                frame.pts,
                frame.extra!![0],
                if (config.mimeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
                    AACPacketType.SEQUENCE_HEADER
                } else {
                    null
                },
                config
            ),
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
    }

    private fun createVideoTags(
        frame: Frame,
        config: VideoConfig
    ): List<FlvTag> {
        val videoTags = mutableListOf<FlvTag>()

        if (frame.isKeyFrame) {
            videoTags.add(
                VideoTagFactory(
                    frame.pts,
                    frame.extra!!,
                    true,
                    PacketType.SEQUENCE_START,
                    config.mimeType
                ).build()
            )
        }

        videoTags.add(
            VideoTagFactory(
                frame.pts,
                frame.buffer,
                frame.isKeyFrame,
                PacketType.CODED_FRAMES_X, // For extended codec onlu.
                config.mimeType
            ).build()
        )

        return videoTags
    }
}
