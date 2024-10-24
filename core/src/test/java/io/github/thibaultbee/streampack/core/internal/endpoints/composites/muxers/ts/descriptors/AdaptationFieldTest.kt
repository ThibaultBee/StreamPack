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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.descriptors

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.TSResourcesUtils
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Test

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
}