/*
 * Copyright 2022 The Android Open Source Project
 * Copyright 2025 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.elements.utils.time

import android.os.SystemClock
import java.util.concurrent.TimeUnit


/**
 * The time provider used to provider timestamps.
 *
 *
 * There are two sets of methods based on [Timebase.UPTIME] and [Timebase.REALTIME].
 *
 * @see SystemTimeProvider
 */
interface TimeProvider {
    /** Returns the timestamp in microseconds based on [Timebase.UPTIME].  */
    fun uptimeUs(): Long = TimeUnit.NANOSECONDS.toMicros(uptimeNs())

    /** Returns the timestamp in nanoseconds based on [Timebase.UPTIME].  */
    fun uptimeNs(): Long

    /** Returns the timestamp in microseconds based on [Timebase.REALTIME].  */
    fun realtimeUs(): Long = TimeUnit.NANOSECONDS.toMicros(realtimeNs())

    /** Returns the timestamp in nanoseconds based on [Timebase.REALTIME].  */
    fun realtimeNs(): Long
}

/**
 * Returns the timestamp in microseconds for the given [timebase].
 */
fun TimeProvider.timeUs(timebase: Timebase): Long {
    return when (timebase) {
        Timebase.UPTIME -> uptimeUs()
        Timebase.REALTIME -> realtimeUs()
    }
}

/**
 * Returns the timestamp in nanoseconds for the given [timebase].
 */
fun TimeProvider.timeNs(timebase: Timebase): Long {
    return when (timebase) {
        Timebase.UPTIME -> uptimeNs()
        Timebase.REALTIME -> realtimeNs()
    }
}

/**
 * A TimeProvider implementation based on System time.
 */
class SystemTimeProvider : TimeProvider {
    override fun uptimeNs() = System.nanoTime()

    override fun realtimeNs() = SystemClock.elapsedRealtimeNanos()
}