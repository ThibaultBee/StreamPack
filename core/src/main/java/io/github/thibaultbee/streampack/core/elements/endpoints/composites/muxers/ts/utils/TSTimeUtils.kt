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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.utils

object TSTimeUtils {
    private const val PCR_BASE_MASK = 0x1FFFFFFFFL // 33 bits mask

    /**
     * Converts a timestamp in microseconds to a 33-bit 90 kHz timestamp.
     * Used for PCR Base, PTS, and DTS.
     */
    fun computeTimestamp90kHz(timestamp: Long): Long {
        // 90 kHz ticks per microsecond = 90,000 / 1,000,000 = 9 / 100
        return (timestamp * 9 / 100) and PCR_BASE_MASK
    }

    /**
     * Converts a timestamp in microseconds to a 9-bit 27 MHz extension clock.
     * Used for PCR Ext.
     */
    fun computePcrExt(timestamp: Long): Long {
        // To convert microseconds to clock ticks, we use fractions to avoid early integer overflow.
        // 27 MHz ticks per microsecond = 27,000,000 / 1,000,000 = 27
        return (timestamp * 27 % 300)
    }
}
