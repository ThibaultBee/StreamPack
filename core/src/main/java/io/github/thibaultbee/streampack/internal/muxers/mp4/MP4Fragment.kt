/*
package io.github.thibaultbee.streampack.internal.muxers.mp4

import io.github.thibaultbee.streampack.internal.data.Frame
import io.github.thibaultbee.streampack.internal.muxers.mp4.boxes.MediaDataBox
import io.github.thibaultbee.streampack.internal.muxers.mp4.boxes.MovieFragmentBox
import io.github.thibaultbee.streampack.internal.muxers.mp4.boxes.MovieFragmentHeaderBox

class MP4Fragment(private val tracks: List<MP4Track>) {
    private var sequenceNumber = 1

    fun add(track: MP4Track, frame: Frame) {
        track.add(frame)
    }

    fun write() {
        writeBuffer(createMoof().write())
        try {
            writeBuffer(MediaDataBox(getDataSize()).write())
            tracks.forEach { writeBuffer(it.write()) }
        } catch (e: Exception) { // Nothing to write
        }
    }

    private fun createMoofBox(): MovieFragmentBox {
        val mfhd = MovieFragmentHeaderBox(
            sequenceNumber = sequenceNumber++,
        )
        return MovieFragmentBox(mfhd, tracks.map { it.getTraf() })
    }

}*/
