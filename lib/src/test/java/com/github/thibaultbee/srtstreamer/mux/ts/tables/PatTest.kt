package com.github.thibaultbee.srtstreamer.mux.ts.tables

import com.github.thibaultbee.srtstreamer.mux.ts.data.Service
import com.github.thibaultbee.srtstreamer.mux.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.mux.ts.utils.AssertEqualsSingleBufferMockMuxListener
import org.junit.Test
import java.nio.ByteBuffer

class PatTest {
    /**
     * Assert generated PAT is equivalent to an expected sample
     */
    @Test
    fun testSimplePat() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/pat.ts")!!.readBytes()
        )
        val listener = AssertEqualsSingleBufferMockMuxListener(expectedBuffer)
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