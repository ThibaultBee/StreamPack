/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.models

import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.MediaDataBox
import java.nio.ByteBuffer

/**
 * A class that represent a single segment: MOOV + MDAT or a single fragment: MOOF + MDAT.
 */
class Segment(
    tracks: List<Track>,
    private val movieBoxFactory: AbstractMovieBoxFactory,
    private val onNewSample: (ByteBuffer) -> Unit
) {
    /**
     * True if this segment is a fragment (MOOF + MDAT).
     */
    val isFragment = movieBoxFactory is MovieFragmentBoxFactory

    private val trackChunks = tracks.map { TrackChunks(it, onNewSample) }
    private val validTrackChunks: List<TrackChunks>
        get() = trackChunks.filter { it.isValid }
    private val validDataSize: Int
        get() = validTrackChunks.sumOf { it.dataSize }

    private val dataSize: Int
        get() = trackChunks.sumOf { it.dataSize }
    val hasData: Boolean
        get() = dataSize > 0

    fun getFirstTimestamp(streamPid: Int): Long {
        val trackSegment = getTrackSegment(streamPid)
        return trackSegment.firstTimestamp
    }

    fun add(frame: Frame, streamPid: Int) {
        val trackSegment = getTrackSegment(streamPid)
        trackSegment.add(frame)
    }

    fun write(dataOffset: Long) {
        if (dataSize == 0) {
            return
        }

        onNewSample(
            movieBoxFactory.build(validTrackChunks, dataOffset).toByteBuffer()
        )

        // Only write MDAT header, then write data
        onNewSample(MediaDataBox(validDataSize).writeHeader())
        validTrackChunks.forEach { it.write() }
    }

    private fun getTrackSegment(id: Int): TrackChunks = trackChunks.first { it.track.id == id }
}
