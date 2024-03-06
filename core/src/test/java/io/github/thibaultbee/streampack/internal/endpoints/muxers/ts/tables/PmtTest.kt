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

import android.media.MediaFormat
import io.github.thibaultbee.streampack.data.Config
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.TSResourcesUtils
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.Service
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.Stream
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.data.TsServiceInfo
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.packets.Pmt
import io.github.thibaultbee.streampack.internal.endpoints.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test

class PmtTest {
    /**
     * Assert generated PMT is equivalent to an expected sample
     */
    @Test
    fun `simple pmt test`() {
        val expectedBuffer = TSResourcesUtils.readByteBuffer("pmt.ts")
        val listener = AssertEqualsSingleBufferMockMuxerListener(expectedBuffer)
        val service =
            Service(
                TsServiceInfo(
                    TsServiceInfo.ServiceType.DIGITAL_TV,  // not used in this test
                    id = 0x0001,
                    name = "ServiceName",  // not used in this test
                    providerName = "ProviderName"  // not used in this test
                ),
                pcrPid = 256
            )
        val streams = listOf(
            Stream(
                Config(MediaFormat.MIMETYPE_VIDEO_MPEG2, 2 * 1024 * 1024),
                256
            ),
            Stream(Config(MediaFormat.MIMETYPE_AUDIO_MPEG, 2 * 1024 * 1024), 257)
        )
        Pmt(listener, service, streams, 0x1000).run {
            write()
        }
    }
}