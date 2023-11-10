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
package io.github.thibaultbee.streampack.internal.muxers.flv

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.data.PacketType
import io.github.thibaultbee.streampack.internal.interfaces.ISourceOrientationProvider
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.AVTagsFactory
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.FlvHeader
import io.github.thibaultbee.streampack.internal.muxers.flv.tags.OnMetadata
import io.github.thibaultbee.streampack.internal.utils.TimeUtils
import io.github.thibaultbee.streampack.internal.utils.extensions.isAudio
import io.github.thibaultbee.streampack.internal.utils.extensions.isVideo

class FlvMuxer(
    override var listener: IMuxerListener? = null,
    initialStreams: List<Config>? = null,
    private val writeToFile: Boolean,
) : IMuxer {
    override val helper = FlvMuxerHelper()
    private val streams = mutableListOf<Config>()
    private val hasAudio: Boolean
        get() = streams.any { it.mimeType.isAudio }
    private val hasVideo: Boolean
        get() = streams.any { it.mimeType.isVideo }
    private var startUpTime: Long? = null
    private var hasFirstFrame = false

    init {
        initialStreams?.let { streams.addAll(it) }
    }

    override var sourceOrientationProvider: ISourceOrientationProvider? = null

    override fun encode(frame: Frame, streamPid: Int) {
        if (!hasFirstFrame) {
            // Wait for first frame
            if (hasVideo) {
                // Expected first video key frame
                if (frame.isVideo && frame.isKeyFrame) {
                    startUpTime = frame.pts
                    hasFirstFrame = true
                } else {
                    // Drop
                    return
                }
            } else {
                // Audio only
                startUpTime = frame.pts
                hasFirstFrame = true
            }
        }
        if (frame.pts < startUpTime!!) {
            return
        }

        frame.pts -= startUpTime!!
        val flvTags = AVTagsFactory(frame, streams[streamPid]).build()
        flvTags.forEach {
            listener?.onOutputFrame(
                Packet(
                    it.write(), frame.pts, if (frame.isVideo) {
                        PacketType.VIDEO
                    } else {
                        PacketType.AUDIO
                    }
                )
            )
        }
    }

    override fun addStreams(streamsConfig: List<Config>): Map<Config, Int> {
        val streamMap = mutableMapOf<Config, Int>()
        streams.addAll(streamsConfig)
        requireStreams()
        streams.forEachIndexed { index, config -> streamMap[config] = index }
        return streamMap
    }

    override fun configure(config: Unit) {
        // Nothing to configure
    }

    override fun startStream() {
        // Header
        if (writeToFile) {
            listener?.onOutputFrame(
                Packet(
                    FlvHeader(hasAudio, hasVideo).write(),
                    0
                )
            )
        }

        // Metadata
        listener?.onOutputFrame(
            Packet(
                OnMetadata.fromConfigs(streams, sourceOrientationProvider).write(),
                TimeUtils.currentTime()
            )
        )
    }

    override fun stopStream() {
        startUpTime = null
        hasFirstFrame = false
        streams.clear()
    }

    override fun release() {
        // Nothing to release
    }

    /**
     * Check that there shall be no more than one audio and one video stream
     */
    private fun requireStreams() {
        require(streams.count { it.mimeType.isAudio } <= 1) { "Only one audio stream is supported by FLV" }
        require(streams.count { it.mimeType.isVideo } <= 1) { "Only one video stream is supported by FLV" }
    }

}