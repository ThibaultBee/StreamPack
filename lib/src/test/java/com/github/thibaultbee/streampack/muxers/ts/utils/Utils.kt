package com.github.thibaultbee.streampack.muxers.ts.utils

import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo

object Utils {
    fun fakeServiceInfo() = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "ServiceName",
        "ProviderName"
    )
}