package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.mp4.models

abstract class MP4SegmenterFactory {
    abstract fun build(hasAudio: Boolean, hasVideo: Boolean): MP4Segmenter
}

class DefaultMP4SegmenterFactory : MP4SegmenterFactory() {
    override fun build(hasAudio: Boolean, hasVideo: Boolean): MP4Segmenter {
        return DefaultMP4Segmenter(hasAudio, hasVideo)
    }
}