/*
 * Copyright (C) 2026 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord

/**
 * Timestamps audio buffers by the number of frames read, so consecutive buffers are exactly their
 * duration apart. An AAC encoder moves an input timestamp that is closer than that forward and
 * keeps the offset.
 *
 * The clock starts at the first buffer's measured capture time. It only moves forward, when the
 * measured time stays ahead of it because frames were dropped.
 */
internal class AudioFrameClock(private val sampleRate: Int) {
    private var originUs = Long.MIN_VALUE

    // The measured time can overshoot after a stalled read and settle over a second, so move by
    // the smallest lead seen over a second of frames; a large lead moves it after a tenth.
    private val lead = Lead(MIN_LEAD_US, sampleRate.toLong(), 0)
    private val lossLead = Lead(LOSS_LEAD_US, sampleRate / 10L, OVERSHOOT_US)

    fun reset() {
        originUs = Long.MIN_VALUE
    }

    /**
     * Returns the timestamp of a buffer of [frames] frames starting at frame [framesRead].
     *
     * @param measuredUs the capture time of frame [framesRead], or -1 if unknown
     * @param fallbackUs its estimate, used only when the clock starts without [measuredUs]
     */
    fun timestampInUs(framesRead: Long, frames: Int, measuredUs: Long, fallbackUs: Long): Long {
        if (originUs == Long.MIN_VALUE) {
            originUs = (if (measuredUs >= 0) measuredUs else fallbackUs) - framesToUs(framesRead)
        }
        val timestampUs = originUs + framesToUs(framesRead)
        if (measuredUs < 0) {
            return timestampUs
        }

        val aheadUs = measuredUs - timestampUs
        val moveUs = maxOf(lead.moveUs(aheadUs, frames), lossLead.moveUs(aheadUs, frames))
        if (moveUs <= 0) {
            return timestampUs
        }
        lead.reset()
        lossLead.reset()
        originUs += moveUs
        return timestampUs + moveUs
    }

    private fun framesToUs(frames: Long) = frames * 1_000_000L / sampleRate

    /**
     * A lead above [thresholdUs] on every read for [minFrames] frames.
     */
    private class Lead(
        private val thresholdUs: Long,
        private val minFrames: Long,
        private val marginUs: Long
    ) {
        private var frames = 0L
        private var minUs = 0L

        fun reset() {
            frames = 0
        }

        /**
         * Returns how far to move the clock forward, or 0.
         */
        fun moveUs(aheadUs: Long, readFrames: Int): Long {
            if (aheadUs <= thresholdUs) {
                frames = 0
                return 0
            }
            minUs = if (frames == 0L) aheadUs else minOf(minUs, aheadUs)
            frames += readFrames
            return if (frames >= minFrames) minUs - marginUs else 0
        }
    }

    companion object {
        // Above getTimestamp() jitter, which reaches ~25 ms on some devices.
        private const val MIN_LEAD_US = 40_000L
        private const val LOSS_LEAD_US = 200_000L
        private const val OVERSHOOT_US = 60_000L
    }
}
