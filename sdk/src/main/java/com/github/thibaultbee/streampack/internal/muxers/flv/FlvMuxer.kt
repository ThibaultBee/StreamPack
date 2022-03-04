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
package com.github.thibaultbee.streampack.internal.muxers.flv

import android.content.Context
import com.github.thibaultbee.streampack.data.Config
import com.github.thibaultbee.streampack.internal.data.Frame
import com.github.thibaultbee.streampack.internal.data.Packet
import com.github.thibaultbee.streampack.internal.muxers.IMuxer
import com.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import com.github.thibaultbee.streampack.internal.muxers.flv.packet.FlvHeader
import com.github.thibaultbee.streampack.internal.muxers.flv.packet.FlvTagFactory
import com.github.thibaultbee.streampack.internal.muxers.flv.packet.OnMetadata
import com.github.thibaultbee.streampack.internal.utils.TimeUtils
import com.github.thibaultbee.streampack.internal.utils.isAudio
import com.github.thibaultbee.streampack.internal.utils.isVideo

class FlvMuxer(
    private val context: Context,
    override var listener: IMuxerListener? = null,
    initialStreams: List<Config>? = null,
    private val writeToFile: Boolean,
) : IMuxer {
    private val streams = mutableListOf<Config>()
    private val hasAudio: Boolean
        get() = streams.any { it.mimeType.isAudio() }
    private val hasVideo: Boolean
        get() = streams.any { it.mimeType.isVideo() }
    private var writeSequenceHeader: Boolean = true

    init {
        initialStreams?.let { streams.addAll(it) }
    }

    override var manageVideoOrientation: Boolean = false

    override fun encode(frame: Frame, streamPid: Int) {
        val flvTags = FlvTagFactory(frame, writeSequenceHeader, streams[streamPid]).build()
        flvTags.forEach {
            listener?.onOutputFrame(Packet(it.write(), frame.pts))
        }
        //   writeSequenceHeader = false
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
                OnMetadata(context, manageVideoOrientation, streams).write(),
                TimeUtils.currentTime()
            )
        )
    }

    override fun stopStream() {
        writeSequenceHeader = true
        streams.clear()
    }

    override fun release() {
        // Nothing to release
    }

    /**
     * Check that there shall be no more than one audio and one video stream
     */
    private fun requireStreams() {
        require(streams.count { it.mimeType.isAudio() } <= 1) { "Only one audio stream is supported by FLV" }
        require(streams.count { it.mimeType.isVideo() } <= 1) { "Only one video stream is supported by FLV" }
    }

}