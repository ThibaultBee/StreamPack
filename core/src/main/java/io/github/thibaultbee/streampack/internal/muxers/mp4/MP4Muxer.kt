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
package io.github.thibaultbee.streampack.internal.muxers.mp4

import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.data.Packet
import io.github.thibaultbee.streampack.internal.muxers.IMuxer
import io.github.thibaultbee.streampack.internal.muxers.IMuxerListener
import io.github.thibaultbee.streampack.internal.muxers.mp4.boxes.*
import io.github.thibaultbee.streampack.internal.utils.isVideo
import java.nio.ByteBuffer

class MP4Muxer(initialListener: IMuxerListener? = null) : IMuxer {
    override val helper = MP4MuxerHelper()
    override var manageVideoOrientation: Boolean = false

    override var listener: IMuxerListener? = initialListener
    private val tracks = mutableListOf<MP4Track>()
    private var fileOffset: Long = 0
    private var sequenceNumber = 0
    private var hasWriteMoov = false

    override fun encode(frame: Frame, streamPid: Int) {
        val track = getTrack(streamPid)
        synchronized(this) {
            if (shouldWriteFragment(frame)) {
                writeFragment()
            }
            track.add(frame)
        }
    }

    override fun addStreams(streamsConfig: List<Config>): Map<Config, Int> {
        val newTrack = mutableListOf<MP4Track>()
        streamsConfig.forEach { config ->
            newTrack.add(MP4Track(getNewId(), config) { buffer -> writeBuffer(buffer) })
        }

        tracks.addAll(newTrack)

        val streamMap = mutableMapOf<Config, Int>()
        newTrack.forEach { streamMap[it.config] = it.id }
        return streamMap
    }

    override fun configure(config: Unit) {
    }

    override fun startStream() {
        writeBuffer(FileTypeBox().write())
    }

    override fun stopStream() {
        writeFragment()
        // TODO write mfra
    }

    override fun release() {
    }

    private fun getNewId(): Int {
        val currentIds = tracks.map { it.id }
        (1..Int.MAX_VALUE).forEach {
            if (!currentIds.contains(it)) {
                return it
            }
        }

        throw IndexOutOfBoundsException("No empty ID left")
    }

    private fun getTrack(id: Int): MP4Track {
        return tracks.first { it.id == id }
    }

    private fun writeFragment() {
        if (tracks.sumOf { it.dataSize } != 0) {
            if (hasWriteMoov) {
                writeBuffer(createMoof().write())
            } else {
                writeBuffer(createMoov().write())
                hasWriteMoov = true
            }
            writeBuffer(MediaDataBox(getDataSize()).write())
            tracks.forEach { it.write() }
        }
    }

    private fun getDataSize(): Int {
        return tracks.sumOf { it.dataSize }
    }

    private fun writeBuffer(buffer: ByteBuffer) {
        val size = buffer.remaining()
        val packet = Packet(buffer, 0)
        listener?.let {
            it.onOutputFrame(packet)
            fileOffset += size
        }
    }

    private fun createMoov(): MovieBox {
        val timescale = try {
            tracks.first { it.config.mimeType.isVideo() }.timescale
        } catch (e: NoSuchElementException) {
            tracks[0].timescale
        }
        val mvhd = MovieHeaderBox(
            version = 0,
            duration = tracks.maxOf { it.totalDuration * timescale / it.timescale },
            timescale = timescale,
            nextTrackId = tracks[0].id
        )
        val moov = MovieBox(
            mvhd,
            tracks.map { it.createTrak(0) },
            MovieExtendsBox(tracks.map { it.createTref() })
        )

        moov.trak.forEach { trak ->
            trak.mdia.minf.stbl.co64.updateFirstChunkOffset(fileOffset + moov.size + 8) // 8 - MediaBoxHeader // TODO: multitrak
        }
        return moov
    }

    private fun createMoof(): MovieFragmentBox {
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = sequenceNumber++,
        )
        return MovieFragmentBox(mfhd, tracks.map { it.createTraf(fileOffset, getMoofSize()) })
    }

    private fun getMoofSize(): Int {
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = sequenceNumber++,
        )
        return MovieFragmentBox(mfhd, tracks.map { it.createTraf(fileOffset, 0) }).size
    }

    private fun shouldWriteFragment(frame: Frame): Boolean {
        return frame.mimeType.isVideo() && frame.isKeyFrame
    }
}