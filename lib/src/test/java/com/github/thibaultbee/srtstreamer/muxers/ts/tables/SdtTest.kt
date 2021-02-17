package com.github.thibaultbee.srtstreamer.muxers.ts.tables

import com.github.thibaultbee.srtstreamer.muxers.ts.data.Service
import com.github.thibaultbee.srtstreamer.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.muxers.ts.utils.AssertEqualsSingleBufferMockMuxerListener
import org.junit.Test
import java.nio.ByteBuffer

class SdtTest {
    /**
     * Assert generated Sdt is equivalent to an expected sample
     */
    @Test
    fun testSimpleSdt() {
        val expectedBuffer = ByteBuffer.wrap(
            this.javaClass.classLoader!!.getResource("test-samples/sdt.ts")!!.readBytes()
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