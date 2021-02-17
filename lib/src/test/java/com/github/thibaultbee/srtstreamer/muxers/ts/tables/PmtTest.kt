package com.github.thibaultbee.srtstreamer.muxers.ts.tables

import android.media.MediaFormat
import com.github.thibaultbee.srtstreamer.muxers.ts.data.Service
import com.github.thibaultbee.srtstreamer.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.muxers.ts.data.Stream
import com.github.thibaultbee.srtstreamer.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test
import java.nio.ByteBuffer

class PmtTest {
    /**
     * Assert generated PMT is equivalent to an expected sample
     */
    @Test
    fun testSimplePmt() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/pmt.ts")!!.readBytes()
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