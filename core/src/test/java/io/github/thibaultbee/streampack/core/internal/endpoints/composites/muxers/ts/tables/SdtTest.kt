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
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.Service
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.data.TSServiceInfo
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.packets.Sdt
import io.github.thibaultbee.streampack.core.internal.endpoints.composites.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test

class SdtTest {
    /**
     * Assert generated Sdt is equivalent to an expected sample
     */
    @Test
    fun `simple sdt test`() {
        val expectedBuffer = TSResourcesUtils.readByteBuffer("sdt.ts")
        val listener = AssertEqualsSingleBufferMockMuxerListener(expectedBuffer)
        val services = listOf(
            Service(
                TSServiceInfo(
                    TSServiceInfo.ServiceType.DIGITAL_TV,
                    id = 0x0001,
                    name = "ServiceName",
                    providerName = "ProviderName"
                )
            )
        )
        Sdt(listener, services, 0x0001, 0xff01.toShort()).run {
            write()
        }
    }
}