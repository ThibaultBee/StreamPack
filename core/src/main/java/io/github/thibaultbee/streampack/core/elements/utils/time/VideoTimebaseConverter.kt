package io.github.thibaultbee.streampack.core.elements.utils.time

import io.github.thibaultbee.streampack.core.logger.Logger
import kotlin.math.abs
import kotlin.math.max

/**
 * Converts video timestamps to [Timebase.UPTIME] if necessary.
 */
class VideoTimebaseConverter(
    private val inputTimebase: Timebase,
    private val timeProvider: TimeProvider
) {
    private var resolvedInputTimebase: Timebase? = null

    private var uptimeToRealtimeOffsetNs = -1L

    /**
     * Converts the video timestamp to [Timebase.UPTIME] if necessary.
     *
     * @param timestampNs the video frame timestamp in nano seconds. The timebase is supposed
     * to be the input timebase in constructor.
     */
    fun convertToUptimeNs(timestampNs: Long): Long {
        if (resolvedInputTimebase == null) {
            resolvedInputTimebase = resolveInputTimebase(timestampNs)
        }
        return when (resolvedInputTimebase) {
            Timebase.REALTIME -> {
                if (uptimeToRealtimeOffsetNs == -1L) {
                    uptimeToRealtimeOffsetNs = calculateUptimeToRealtimeOffsetNs()
                }
                timestampNs - uptimeToRealtimeOffsetNs
            }

            Timebase.UPTIME -> timestampNs

            else -> throw AssertionError("Unknown timebase: $resolvedInputTimebase")
        }
    }

    private fun resolveInputTimebase(timestampNs: Long): Timebase {
        Logger.i(TAG, "Resolving input timebase...for $timestampNs")
        if (!exceedUptimeRealtimeDiffThreshold()) {
            return inputTimebase
        }

        val resolvedTimebase = if (isCloseToRealtime(timestampNs)) {
            Timebase.REALTIME
        } else {
            Timebase.UPTIME
        }
        if (resolvedTimebase != inputTimebase) {
            Logger.e(
                TAG, "System time diverged, detected timebase $resolvedTimebase " +
                        "different from configured input timebase $inputTimebase"
            )
        }

        Logger.i(TAG, "Detect input timebase = $resolvedTimebase")
        return resolvedTimebase
    }

    private fun exceedUptimeRealtimeDiffThreshold(): Boolean {
        val uptimeUs = timeProvider.uptimeUs()
        val realTimeUs = timeProvider.realtimeUs()
        return (realTimeUs - uptimeUs) > UPTIME_REALTIME_DIFF_THRESHOLD_US
    }

    private fun isCloseToRealtime(timeNs: Long): Boolean {
        val uptimeNs = timeProvider.uptimeNs()
        val realtimeNs = timeProvider.realtimeNs()
        return abs(timeNs - realtimeNs) < abs(timeNs - uptimeNs)
    }

    // The algorithm is from camera framework Camera3Device.cpp
    private fun calculateUptimeToRealtimeOffsetNs(): Long {
        // Try three times to get the clock offset, choose the one with the minimum gap in
        // measurements.
        var bestGap = Long.MAX_VALUE
        var measured = 0L
        repeat(CLOCK_OFFSET_CALIBRATION_ATTEMPTS) { i ->
            val uptime1: Long = timeProvider.uptimeNs()
            val realtime: Long = timeProvider.realtimeNs()
            val uptime2: Long = timeProvider.uptimeNs()
            val gap = uptime2 - uptime1
            if (i == 0 || gap < bestGap) {
                bestGap = gap
                measured = realtime - ((uptime1 + uptime2) shr 1)
            }
        }
        return max(0, measured)
    }

    companion object {
        private const val TAG = "VideoTimebaseConverter"

        private const val UPTIME_REALTIME_DIFF_THRESHOLD_US = 100000L // 100 mseconds
        private const val CLOCK_OFFSET_CALIBRATION_ATTEMPTS = 3
    }
}