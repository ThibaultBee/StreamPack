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

import io.github.thibaultbee.streampack.internal.endpoints.muxers.mp4.boxes.*

abstract class AbstractMovieBoxFactory {
    abstract fun build(trackChunks: List<TrackChunks>, dataOffset: Long): Box
}

class MovieBoxFactory(val timescale: Int) : AbstractMovieBoxFactory() {
    override fun build(trackChunks: List<TrackChunks>, dataOffset: Long): Box {
        val mvhd = MovieHeaderBox(
            version = 0,
            duration = trackChunks.maxOf { it.duration },
            timescale = timescale,
            nextTrackId = trackChunks[0].track.id
        )
        val moov = MovieBox(
            mvhd,
            trackChunks.map { it.createTrak(0L) },
            MovieExtendsBox(trackChunks.map { it.createTref() })
        )

        var mdatOffset = dataOffset + moov.size + 8 /*Mdat header*/
        moov.trak.forEachIndexed { index, trak ->
            trak.mdia.minf.stbl.co64.addChunkOffset(mdatOffset) // 8 - MediaBoxHeader // TODO: multitrak
            mdatOffset += trackChunks[index].dataSize
        }
        return moov
    }
}

class MovieFragmentBoxFactory(private val sequenceNumber: Int) : AbstractMovieBoxFactory() {
    override fun build(trackChunks: List<TrackChunks>, dataOffset: Long): Box {
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = sequenceNumber,
        )
        val moofSize = getMoofSize(trackChunks)
        var mdatOffset = moofSize + 8 /*Mdat header*/
        // TODO: update data size dynamically
        return MovieFragmentBox(
            mfhd,
            trackChunks.map {
                it.createTraf(dataOffset, mdatOffset).also { _ ->
                    mdatOffset += it.dataSize
                }
            })
    }

    private fun getMoofSize(tracksSegment: List<TrackChunks>): Int {
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = 0,
        )
        return MovieFragmentBox(mfhd, tracksSegment.map { it.createTraf(0, 0) }).size
    }
}