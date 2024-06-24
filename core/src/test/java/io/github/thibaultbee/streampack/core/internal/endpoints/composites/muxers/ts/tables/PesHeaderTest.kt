/*
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
package io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.tables

import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.TSResourcesUtils
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.packets.PesHeader
import io.github.thibaultbee.streampack.core.internal.utils.extensions.toByteArray
import org.junit.Assert
import org.junit.Test

class PesHeaderTest {
    @Test
    fun `pes header with pts and dts test`() {
        val expectedPesHeader = TSResourcesUtils.readByteBuffer("pes-header.ts")

        val pesHeader = PesHeader(
            streamId = 224,
            payloadLength = 32562,
            esScramblingControl = 0,
            esPriority = false,
            dataAlignmentIndicator = false,
            copyright = false,
            originalOrCopy = true,
            pts = 1433334,
            dts = 1400000,
            esClockReference = null,
            esRate = null,
            dsmTrickMode = null,
            additionalCopyInfo = null
        )

        Assert.assertArrayEquals(
            expectedPesHeader.array(),
            pesHeader.toByteBuffer().toByteArray()
        )
    }
}