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

import android.media.MediaFormat
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Service
import com.github.thibaultbee.streampack.internal.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.internal.muxers.ts.data.Stream
import com.github.thibaultbee.streampack.internal.muxers.ts.packets.Pmt
import com.github.thibaultbee.streampack.internal.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test
import java.nio.ByteBuffer

class PmtTest {
    /**
     * Assert generated PMT is equivalent to an expected sample
     */
    @Test
    fun testSimplePmt() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/muxer/pmt.ts")!!.readBytes()
        )
        val listener = AssertEqualsSingleBufferMockMuxerListener(expectedBuffer)
        val service =
            Service(
                ServiceInfo(
                    ServiceInfo.ServiceType.DIGITAL_TV,  // not used in this test
                    id = 0x0001,
                    name = "ServiceName",  // not used in this test
                    providerName = "ProviderName"  // not used in this test
                ),
                pcrPid = 256
            )
        val streams = listOf(
            Stream(MediaFormat.MIMETYPE_VIDEO_MPEG2, 256),
            Stream(MediaFormat.MIMETYPE_AUDIO_MPEG, 257)
        )
        Pmt(listener, service, streams, 0x1000).run {
            write()
        }
    }
}