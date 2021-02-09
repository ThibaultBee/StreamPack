package com.github.thibaultbee.srtstreamer.mux.ts.tables

import com.github.thibaultbee.srtstreamer.mux.ts.data.Service
import com.github.thibaultbee.srtstreamer.mux.ts.data.ServiceInfo
import com.github.thibaultbee.srtstreamer.mux.ts.utils.AssertEqualsSingleBufferMockMuxListener
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
        val listener = AssertEqualsSingleBufferMockMuxListener(expectedBuffer)
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