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
import com.github.thibaultbee.streampack.internal.muxers.ts.packets.Pat
import com.github.thibaultbee.streampack.internal.muxers.ts.packets.Pmt
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test
import java.nio.ByteBuffer

class PatTest {
    /**
     * Assert generated PAT is equivalent to an expected sample
     */
    @Test
    fun testSimplePat() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/muxer/pat.ts")!!.readBytes()
        )
        val listener = AssertEqualsSingleBufferMockMuxerListener(expectedBuffer)
        val service = Service(
            ServiceInfo(
                ServiceInfo.ServiceType.DIGITAL_TV,  // not used in this test
                id = 0x6d66,
                name = "ServiceName",  // not used in this test
                providerName = "ProviderName"  // not used in this test
            ),
            pcrPid = 4567,  // not used in this test
        )
        Pmt(listener, service, emptyList(), 0x64).run { service.pmt = this }

        Pat(listener, listOf(service), tsId = 0x437, versionNumber = 3).run {
            write()
        }

    }
}