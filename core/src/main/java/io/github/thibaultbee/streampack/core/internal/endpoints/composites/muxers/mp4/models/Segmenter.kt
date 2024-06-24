package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.mp4.models

import io.github.thibaultbee.streampack.core.internal.data.Frame

/**
 * A class that is responsible to decide when to write a segment.
 */
abstract class MP4Segmenter(
    protected val hasAudio: Boolean,
    protected val hasVideo: Boolean
) {
    protected val isAudioOnly = hasAudio && !hasVideo
    protected val isVideoOnly = hasVideo && !hasAudio

    abstract fun mustWriteSegment(frame: io.github.thibaultbee.streampack.core.internal.data.Frame): Boolean
}

/**
 * Default implementation of [MP4Segmenter].
 * Return the order to write segment when:
 *  - Audio only: every [numOfAudioSampleInSegment] audio frames
 *  - else, when video frame is a key frame (except the first one)
 */
class DefaultMP4Segmenter(
    hasAudio: Boolean,
    hasVideo: Boolean,
    private val numOfAudioSampleInSegment: Int = DEFAULT_NUM_OF_AUDIO_SAMPLE_IN_SEGMENT
) :
    MP4Segmenter(hasAudio, hasVideo) {
    private var audioFrameCounter = 0
    private var isFirstVideoFrame = true

    override fun mustWriteSegment(frame: io.github.thibaultbee.streampack.core.internal.data.Frame): Boolean {
        return if (isAudioOnly) {
            audioFrameCounter++
            if (audioFrameCounter == numOfAudioSampleInSegment) {
                audioFrameCounter = 0
                true
            } else {
                false
            }
        } else {
            if (frame.isVideo && frame.isKeyFrame) {
                if (isFirstVideoFrame) {
                    isFirstVideoFrame = false
                    false
                } else {
                    true
                }
            } else {
                false
            }
        }
    }

    companion object {
        const val DEFAULT_NUM_OF_AUDIO_SAMPLE_IN_SEGMENT = 128 // Arbitrary value
    }
}