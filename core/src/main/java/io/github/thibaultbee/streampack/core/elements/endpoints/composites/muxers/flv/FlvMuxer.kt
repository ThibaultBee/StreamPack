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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv

import io.github.thibaultbee.streampack.core.elements.data.FrameWithCloseable
import io.github.thibaultbee.streampack.core.elements.data.Packet
import io.github.thibaultbee.streampack.core.elements.encoders.CodecConfig
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.IMuxerInternal
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags.AVTagsFactory
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags.FlvHeader
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.flv.tags.OnMetadata
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isAudio
import io.github.thibaultbee.streampack.core.elements.utils.extensions.isVideo

class FlvMuxer(
    override var listener: IMuxerInternal.IMuxerListener? = null,
    private val isForFile: Boolean,
) : IMuxerInternal {
    override val info by lazy { FlvMuxerInfo }
    private val streams = mutableListOf<Stream>()
    private val hasAudio: Boolean
        get() = streams.any { it.config.mimeType.isAudio }
    private val hasVideo: Boolean
        get() = streams.any { it.config.mimeType.isVideo }
    private var startUpTime: Long? = null
    private var hasFirstFrame = false

    override val streamConfigs: List<CodecConfig>
        get() = streams.map { it.config }

    override fun write(closeableFrame: FrameWithCloseable, streamPid: Int) {
        val frame = closeableFrame.frame
        synchronized(this) {
            try {
                if (!hasFirstFrame) {
                    /**
                     * Wait for first video frame to start (only if video is present)
                     */
                    if (hasVideo) {
                        // Expected first video key frame
                        if (frame.isVideo && frame.isKeyFrame) {
                            startUpTime = frame.ptsInUs
                            hasFirstFrame = true
                        } else {
                            // Drop
                            return
                        }
                    } else {
                        // Audio only
                        startUpTime = frame.ptsInUs
                        hasFirstFrame = true
                    }
                }

                if (frame.ptsInUs < startUpTime!!) {
                    return
                }

                frame.ptsInUs -= startUpTime!!
                val stream = streams[streamPid]
                val sendHeader = stream.sendHeader
                stream.sendHeader = false
                val flvTags = AVTagsFactory(frame, stream.config, sendHeader).build()
                flvTags.forEach {
                    listener?.onOutputFrame(
                        Packet(it.write(), frame.ptsInUs)
                    )
                }
            } finally {
                closeableFrame.close()
            }
        }
    }

    override fun addStreams(streamsConfig: List<CodecConfig>): Map<CodecConfig, Int> {
        val streamMap = mutableMapOf<CodecConfig, Int>()
        streams.addAll(streamsConfig.map {
            Stream(
                it
            )
        })
        requireStreams()
        streams.forEachIndexed { index, stream -> streamMap[stream.config] = index }
        return streamMap
    }

    override fun addStream(streamConfig: CodecConfig): Int {
        streams.add(
            Stream(
                streamConfig
            )
        )
        requireStreams()
        return streams.size - 1
    }

    override fun startStream() {
        // Header
        if (isForFile) {
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
                OnMetadata.fromConfigs(streams.map { it.config }).write(),
                0
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
        val audioStreams = streams.filter { it.config.mimeType.isAudio }
        require(audioStreams.size <= 1) { "Only one audio stream is supported by FLV but got $audioStreams" }
        val videoStreams = streams.filter { it.config.mimeType.isVideo }
        require(videoStreams.size <= 1) { "Only one video stream is supported by FLV but got $videoStreams" }
    }

    private data class Stream(val config: CodecConfig) {
        var sendHeader = true
    }
}
