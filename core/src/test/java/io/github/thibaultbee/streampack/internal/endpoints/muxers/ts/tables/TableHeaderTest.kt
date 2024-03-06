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
package io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.tables

import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.packets.TableHeader
import io.github.thibaultbee.streampack.internal.utils.extensions.toByteArray
import io.github.thibaultbee.streampack.utils.ResourcesUtils
import org.junit.Assert
import org.junit.Test

class TableHeaderTest {
    @Test
    fun `simple table header test`() {
        val expectedTableHeader =
            ResourcesUtils.readByteBuffer("test-samples/muxer/ts/table-header.ts")

        val tableHeader = TableHeader(
            tableId = 2,
            sectionSyntaxIndicator = true,
            reservedFutureUse = false,
            payloadLength = 14,
            tableIdExtension = 1,
            versionNumber = 0,
            currentNextIndicator = true,
            sectionNumber = 0,
            lastSectionNumber = 0
        )

        Assert.assertArrayEquals(
            expectedTableHeader.array(),
            tableHeader.toByteBuffer().toByteArray()
        )
    }
}