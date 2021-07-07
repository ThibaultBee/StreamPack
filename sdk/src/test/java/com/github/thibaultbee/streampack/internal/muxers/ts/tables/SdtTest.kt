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
package com.github.thibaultbee.streampack.internal.muxers.ts.tables

import com.github.thibaultbee.streampack.internal.muxers.ts.data.Service
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.muxers.ts.packets.Sdt
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test
import java.nio.ByteBuffer

class SdtTest {
    /**
     * Assert generated Sdt is equivalent to an expected sample
     */
    @Test
    fun testSimpleSdt() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/muxer/sdt.ts")!!.readBytes()
        )
        val listener = AssertEqualsSingleBufferMockMuxerListener(expectedBuffer)
        val services = listOf(
            Service(
                ServiceInfo(
                    ServiceInfo.ServiceType.DIGITAL_TV,
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