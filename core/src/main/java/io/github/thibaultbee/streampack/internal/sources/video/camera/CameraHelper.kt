/*
 * Copyright 2019 The MediaPipe Authors.
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.internal.sources.video.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.SystemClock
import io.github.thibaultbee.streampack.utils.getCameraCharacteristics


object CameraHelper {
    // Number of attempts for calculating the offset between the camera's clock and MONOTONIC clock.
    private const val CLOCK_OFFSET_CALIBRATION_ATTEMPTS = 3

    // Computes the difference between the camera's clock and MONOTONIC clock using camera's
    // timestamp source information. This function assumes by default that the camera timestamp
    // source is aligned to CLOCK_MONOTONIC. This is useful when the camera is being used
    // synchronously with other sensors that yield timestamps in the MONOTONIC timebase, such as
    // AudioRecord for audio data. The offset is returned in milliseconds.
    fun getTimeOffsetToMonoClock(context: Context, cameraId: String): Long {
        return if (context.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE) == CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME
        ) {
            // This clock shares the same timebase as SystemClock.elapsedRealtimeNanos(), see
            // https://developer.android.com/reference/android/hardware/camera2/CameraMetadata.html#SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME.
            getOffsetFromRealtimeTimestampSource()
        } else {
            getOffsetFromUnknownTimestampSource()
        }
    }

    private fun getOffsetFromUnknownTimestampSource(): Long {
        // Implementation-wise, this timestamp source has the same timebase as CLOCK_MONOTONIC, see
        // https://stackoverflow.com/questions/38585761/what-is-the-timebase-of-the-timestamp-of-cameradevice.
        return 0L
    }

    private fun getOffsetFromRealtimeTimestampSource(): Long {
        // Measure the offset of the REALTIME clock w.r.t. the MONOTONIC clock. Do
        // CLOCK_OFFSET_CALIBRATION_ATTEMPTS measurements and choose the offset computed with the
        // smallest delay between measurements. When the camera returns a timestamp ts, the
        // timestamp in MONOTONIC timebase will now be (ts + cameraTimeOffsetToMonoClock).
        var offset = Long.MAX_VALUE
        var lowestGap = Long.MAX_VALUE
        for (i in 0 until CLOCK_OFFSET_CALIBRATION_ATTEMPTS) {
            val startMonoTs = System.nanoTime()
            val realTs = SystemClock.elapsedRealtimeNanos()
            val endMonoTs = System.nanoTime()
            val gapMonoTs = endMonoTs - startMonoTs
            if (gapMonoTs < lowestGap) {
                lowestGap = gapMonoTs
                offset = (startMonoTs + endMonoTs) / 2 - realTs
            }
        }
        return offset / 1000
    }
}