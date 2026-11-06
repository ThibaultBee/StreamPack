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

import io.github.thibaultbee.streampack.core.elements.sources.audio.audiorecord.AudioRecordSource.Companion.firstFrameTimestampInUs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRecordSourceTimestampTest {
    @Test
    fun `timestamp is the first frame of the buffer just read`() {
        // 1024 frames already read, 1024 just read and 441 still buffered: framePosition 2489.
        assertEquals(
            10_000_000L - (1024 + 441) * 1_000_000L / 44_100,
            firstFrameTimestampInUs(10_000_000_000L, 2489, 1024, 2646 + 1024, 44_100)
        )
    }

    @Test
    fun `framePosition behind the frames just read still extrapolates`() {
        assertEquals(
            10_000_000L - 1_000L,
            firstFrameTimestampInUs(10_000_000_000L, 1048, 1000, 2000, 48_000)
        )
    }

    @Test
    fun `framePosition behind the frames read extrapolates forward`() {
        // A stale timestamp while the reader drains a backlog: frame 4410 was captured at nanoTime,
        // so frame 8820, read after it, was captured 100 ms later.
        assertEquals(
            10_000_000L + 100_000L,
            firstFrameTimestampInUs(10_000_000_000L, 4410, 8820, 44_100, 44_100)
        )
    }

    @Test
    fun `framePosition on the first frame gives nanoTime`() {
        assertEquals(10_000_000L, firstFrameTimestampInUs(10_000_000_000L, 0, 0, 2000, 48_000))
    }

    @Test
    fun `framePosition from another start is rejected`() {
        // Not reset by the restart.
        assertEquals(-1L, firstFrameTimestampInUs(10_000_000_000L, 500_000, 1024, 3670, 44_100))
        // Reset, but frames read were not.
        assertEquals(-1L, firstFrameTimestampInUs(10_000_000_000L, 1024, 500_000, 3670, 44_100))
    }

    @Test
    fun `framesAhead bounds are inclusive on both sides`() {
        assertEquals(
            10_000_000L - 3670 * 1_000_000L / 44_100,
            firstFrameTimestampInUs(10_000_000_000L, 3670, 0, 3670, 44_100)
        )
        assertEquals(-1L, firstFrameTimestampInUs(10_000_000_000L, 3671, 0, 3670, 44_100))
        assertEquals(
            10_000_000L + 3670 * 1_000_000L / 44_100,
            firstFrameTimestampInUs(10_000_000_000L, 0, 3670, 3670, 44_100)
        )
        assertEquals(-1L, firstFrameTimestampInUs(10_000_000_000L, 0, 3671, 3670, 44_100))
    }

    /**
     * Feeds [count] buffers of [FRAMES] frames to [clock], each measured at its sample-clock time
     * plus [offsetUs] of its index, and returns their timestamps.
     */
    private fun AudioFrameClock.feed(
        count: Int,
        firstBuffer: Int = 0,
        fallbackUs: (Int) -> Long = { -1 },
        offsetUs: (Int) -> Long?
    ) = (firstBuffer until firstBuffer + count).map { i ->
        val framesRead = i * FRAMES.toLong()
        val offset = offsetUs(i)
        timestampInUs(
            framesRead,
            FRAMES,
            if (offset == null) -1 else START_US + framesToUs(framesRead) + offset,
            fallbackUs(i)
        )
    }

    private fun framesToUs(frames: Long) = frames * 1_000_000L / RATE

    private fun sampleClock(i: Int) = START_US + framesToUs(i * FRAMES.toLong())

    @Test
    fun `buffers are exactly their duration apart`() {
        val pts = AudioFrameClock(RATE).feed(100) { 0 }
        pts.forEachIndexed { i, t -> assertEquals(sampleClock(i), t) }
        pts.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= BUFFER_US) }
    }

    @Test
    fun `timestamp jitter does not move the clock`() {
        // Bursts 26 ms early or late for a few reads, and a constant lead under the threshold for
        // longer than the persistence.
        val burst = AudioFrameClock(RATE).feed(300) { i ->
            when {
                i > 200 -> 39_000L
                i % 20 == 5 -> 26_000L
                i % 20 == 6 -> -40_000L
                i % 20 == 7 -> -14_000L
                else -> 0L
            }
        }
        burst.forEachIndexed { i, t -> assertEquals(sampleClock(i), t) }
        // A lead above the threshold that is gone within a second.
        val transient = AudioFrameClock(RATE).feed(300) { i ->
            if (i % 60 in 10..50) 150_000L else 0L
        }
        transient.forEachIndexed { i, t -> assertEquals(sampleClock(i), t) }
    }

    @Test
    fun `frame loss moves the clock forward once, by the smallest lead`() {
        // 150 ms of frames lost after buffer 50; the first buffers after the stall overshoot, and
        // the smallest lead is not the latest.
        val pts = AudioFrameClock(RATE).feed(300) { i ->
            when {
                i < 50 -> 0L
                i < 53 -> 190_000L
                i < 80 -> 150_000L
                else -> 155_000L
            }
        }
        val moved = pts.indices.filter { pts[it] != sampleClock(it) }
        // One second of frames is 44100 / 1024 = 43.07 buffers: the 44th buffer from 50 moves it.
        assertEquals(50 + 43, moved.first())
        moved.forEach { assertEquals(sampleClock(it) + 150_000L, pts[it]) }
        assertEquals(300 - 93, moved.size)
        pts.zipWithNext().forEach { (a, b) -> assertTrue(b - a >= BUFFER_US) }
    }

    @Test
    fun `a large frame loss moves the clock within a tenth of a second, the rest after a second`() {
        val pts = AudioFrameClock(RATE).feed(300) { i ->
            when {
                i < 50 -> 0L
                i < 53 -> 560_000L
                else -> 500_000L
            }
        }
        // A tenth of a second is 4.3 buffers: the 5th from 50 moves it, keeping 60 ms back.
        (0 until 54).forEach { assertEquals(sampleClock(it), pts[it]) }
        (54 until 98).forEach { assertEquals(sampleClock(it) + 440_000L, pts[it]) }
        // The remaining 60 ms lead lasts a second from buffer 55.
        (98 until 300).forEach { assertEquals(sampleClock(it) + 500_000L, pts[it]) }
    }

    @Test
    fun `the clock never moves backward`() {
        val pts = AudioFrameClock(RATE).feed(300) { i -> if (i < 10) 0L else -300_000L }
        pts.forEachIndexed { i, t -> assertEquals(sampleClock(i), t) }
    }

    @Test
    fun `missing measurements keep counting frames`() {
        val clock = AudioFrameClock(RATE)
        // Starts on the fallback, then ignores it even as it runs ahead.
        val pts = clock.feed(100, fallbackUs = { sampleClock(it) + 500_000L + it * 2_000L }) { null }
        pts.forEachIndexed { i, t -> assertEquals(sampleClock(i) + 500_000L, t) }
        // Measurements behind the clock that started late do not move it either.
        val later = clock.feed(100, firstBuffer = 100) { 0L }
        later.forEachIndexed { i, t -> assertEquals(sampleClock(100 + i) + 500_000L, t) }
    }

    @Test
    fun `the clock starts on the first measurement, not the fallback`() {
        val pts = AudioFrameClock(RATE).feed(10, fallbackUs = { sampleClock(it) + 500_000L }) { 0L }
        pts.forEachIndexed { i, t -> assertEquals(sampleClock(i), t) }
    }

    @Test
    fun `reset starts the clock again`() {
        val clock = AudioFrameClock(RATE)
        clock.feed(100) { 0L }
        clock.reset()
        // A restart counts frames from 0 again, captured 10 s later.
        val pts = clock.feed(10) { 10_000_000L }
        pts.forEachIndexed { i, t -> assertEquals(sampleClock(i) + 10_000_000L, t) }
    }

    private companion object {
        const val RATE = 44_100
        const val FRAMES = 1024
        const val BUFFER_US = FRAMES * 1_000_000L / RATE
        const val START_US = 1_000_000_000L
    }
}
