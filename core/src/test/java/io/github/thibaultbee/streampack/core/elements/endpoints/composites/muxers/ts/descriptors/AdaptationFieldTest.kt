/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.descriptors

import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.TSResourcesUtils
import io.github.thibaultbee.streampack.core.elements.endpoints.composites.muxers.ts.utils.TSConst
import io.github.thibaultbee.streampack.core.elements.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class AdaptationFieldTest {

    @Test
    fun `adaptation field with pcr`() {
        val expectedAdaptationField =
            TSResourcesUtils.readByteBuffer("adaptation-field.ts")

        val adaptationField = AdaptationField(
            discontinuityIndicator = false,
            randomAccessIndicator = true,
            elementaryStreamPriorityIndicator = false,
            programClockReference = 13895163261,
            originalProgramClockReference = null,
            spliceCountdown = null,
            transportPrivateData = null,
            adaptationFieldExtension = null
        )

        assertArrayEquals(
            expectedAdaptationField.array(),
            adaptationField.toByteBuffer().toByteArray()
        )
    }

    @Test
    fun `pcr does not overflow after long device uptime`() {
        // 400_000_000_000 microseconds ~= 4.63 days. With the pre-fix formula
        // (SYSTEM_CLOCK_FREQ * timestamp evaluated before any division),
        // 27_000_000 * 400_000_000_000 = 1.08e19 overflows Long.MAX_VALUE
        // (~9.223e18) and silently wraps in Kotlin/JVM, corrupting the PCR of
        // every frame past ~95h of uptime (~3.4e11 us). This reproduces that
        // magnitude and checks the encoded PCR against a ground truth computed
        // with BigInteger (same original semantics, immune to Long overflow at
        // this size) instead of against the patched formula itself.
        val timestamp = 400_000_000_000L

        val adaptationField = AdaptationField(
            discontinuityIndicator = false,
            randomAccessIndicator = true,
            elementaryStreamPriorityIndicator = false,
            programClockReference = timestamp,
            originalProgramClockReference = null,
            spliceCountdown = null,
            transportPrivateData = null,
            adaptationFieldExtension = null
        )

        val bytes = adaptationField.toByteBuffer()
        bytes.position(2) // skip adaptation_field_length + flags byte
        val pcrBaseHigh32 = bytes.int.toLong() and 0xFFFFFFFFL
        val tail = bytes.short.toInt() and 0xFFFF
        val actualPcrBase = (pcrBaseHigh32 shl 1) or ((tail.toLong() shr 15) and 0x1L)
        val actualPcrExt = tail and 0x1FF

        val freq = BigInteger.valueOf(TSConst.SYSTEM_CLOCK_FREQ.toLong())
        val ts = BigInteger.valueOf(timestamp)
        val twoPow33 = BigInteger.ONE.shiftLeft(33)
        val expectedPcrBase =
            freq.multiply(ts).divide(BigInteger.valueOf(1_000_000)).divide(BigInteger.valueOf(300))
                .mod(twoPow33).toLong()
        val expectedPcrExt =
            freq.multiply(ts).divide(BigInteger.valueOf(1_000_000)).mod(BigInteger.valueOf(300)).toInt()

        assertEquals(expectedPcrBase, actualPcrBase)
        assertEquals(expectedPcrExt, actualPcrExt)
    }
}